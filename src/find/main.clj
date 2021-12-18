(ns find.main
  (:gen-class)
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >! <!! >!!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.string]
   [clojure.pprint :refer [pprint]]
   [clojure.walk]
   [clojure.java.io :as io]

   [find.fs]
   [find.protocols]

   [find.bytes]
   [find.codec]
   [find.bencode]

   [find.seed]

   [find.db :as find.db]

   [find.bittorrent-dht :as find.bittorrent-dht]
   [find.bittorrent-find-nodes :as find.bittorrent-find-nodes]
   [find.bittorrent-metadata :as find.bittorrent-metadata]
   [find.bittorrent-sybil :as find.bittorrent-sybil]
   [find.bittorrent-sample-infohashes :as find.bittorrent-sample-infohashes])
  (:import
   (javax.swing JFrame JLabel JButton SwingConstants JMenuBar JMenu JTextArea)
   (java.awt Canvas Graphics)
   (java.awt.event WindowListener KeyListener KeyEvent)))

(println "clojure.core.async.pool-size" (System/getProperty "clojure.core.async.pool-size"))
(println "clojure.compiler.direct-linking" (System/getProperty "clojure.compiler.direct-linking"))
(set! *warn-on-reflection* true)

(defonce stateA (atom {:searchS ""}))

(declare
 process-print-info
 process-count)

(defn -main [& args]
  (println :-main)
  (let [data-dir (find.fs/path-join (System/getProperty "user.dir") "data")
        state-filepath (find.fs/path-join data-dir "find.json")
        _ (swap! stateA merge
                 (let [self-idBA  (find.codec/hex-to-bytes "a8fb5c14469fc7c46e91679c493160ed3d13be3d") #_(find.bytes/random-bytes 20)]
                   {:self-id (find.codec/hex-to-string self-idBA)
                    :self-idBA self-idBA
                    :routing-table (sorted-map)
                    :dht-keyspace {}
                    :routing-table-sampled {}
                    :routing-table-find-noded {}})
                 (<!! (find.seed/read-state-file state-filepath)))

        self-id (:self-id @stateA)
        self-idBA (:self-idBA @stateA)

        port 6881
        host "0.0.0.0"

        count-messagesA (atom 0)

        msg| (chan (sliding-buffer 100)
                   (keep (fn [{:keys [msgBA host port]}]
                           (swap! count-messagesA inc)
                           (try
                             {:msg  (->
                                     (find.bencode/decode msgBA)
                                     (clojure.walk/keywordize-keys))
                              :host host
                              :port port}
                             (catch Exception ex nil)))))

        msg|mult (mult msg|)

        torrent| (chan 5000)
        torrent|mult (mult torrent|)

        send| (chan 1000)

        unique-infohashsesA (atom #{})
        xf-infohash (comp
                     (map (fn [{:keys [infohashBA] :as value}]
                            (assoc value :infohash (find.codec/hex-to-string infohashBA))))
                     (filter (fn [{:keys [infohash]}]
                               (not (get @unique-infohashsesA infohash))))
                     (map (fn [{:keys [infohash] :as value}]
                            (swap! unique-infohashsesA conj infohash)
                            value)))

        infohashes-from-sampling| (chan (sliding-buffer 100000) xf-infohash)
        infohashes-from-listening| (chan (sliding-buffer 100000) xf-infohash)
        infohashes-from-sybil| (chan (sliding-buffer 100000) xf-infohash)

        infohashes-from-sampling|mult (mult infohashes-from-sampling|)
        infohashes-from-listening|mult (mult infohashes-from-listening|)
        infohashes-from-sybil|mult (mult infohashes-from-sybil|)

        nodesBA| (chan (sliding-buffer 100))

        send-krpc-request (find.seed/send-krpc-request-fn {:msg|mult msg|mult
                                                           :send| send|})

        valid-node? (fn [node]
                      (and
                       (:host node)
                       (:port node)
                       (:id node)
                       (not= (:host node) host)
                       (not= (:id node) self-id)
                       #_(not= 0 (js/Buffer.compare (:id node) self-id))
                       (< 0 (:port node) 65536)))

        routing-table-nodes| (chan (sliding-buffer 1024)
                                   (map (fn [nodes] (filter valid-node? nodes))))

        dht-keyspace-nodes| (chan (sliding-buffer 1024)
                                  (map (fn [nodes] (filter valid-node? nodes))))

        xf-node-for-sampling? (comp
                               (filter valid-node?)
                               (filter (fn [node] (not (get (:routing-table-sampled @stateA) (:id node)))))
                               (map (fn [node] [(:id node) node])))

        nodes-to-sample| (chan (find.seed/sorted-map-buffer 10000 (find.seed/hash-key-distance-comparator-fn  self-idBA))
                               xf-node-for-sampling?)

        nodes-from-sampling| (chan (find.seed/sorted-map-buffer 10000 (find.seed/hash-key-distance-comparator-fn  self-idBA))
                                   xf-node-for-sampling?)

        duration (* 10 60 1000)
        nodes-bootstrap [{:host "router.bittorrent.com"
                          :port 6881}
                         {:host "dht.transmissionbt.com"
                          :port 6881}
                         #_{:host "dht.libtorrent.org"
                            :port 25401}]

        sybils| (chan 30000)

        procsA (atom [])
        release (fn []
                  (let [stop|s @procsA]
                    (doseq [stop| stop|s]
                      (close! stop|))
                    (close! msg|)
                    (close! torrent|)
                    (close! infohashes-from-sampling|)
                    (close! infohashes-from-listening|)
                    (close! infohashes-from-sybil|)
                    (close! nodes-to-sample|)
                    (close! nodes-from-sampling|)
                    (close! nodesBA|)
                    (a/merge stop|s)))

        _ (swap! stateA merge
                 {:stateA stateA
                  :host host
                  :port port
                  :data-dir data-dir
                  :self-id self-id
                  :self-idBA self-idBA
                  :msg| msg|
                  :msg|mult msg|mult
                  :send| send|
                  :torrent| torrent|
                  :torrent|mult torrent|mult
                  :nodes-bootstrap nodes-bootstrap
                  :nodes-to-sample| nodes-to-sample|
                  :nodes-from-sampling| nodes-from-sampling|
                  :routing-table-nodes| routing-table-nodes|
                  :dht-keyspace-nodes| dht-keyspace-nodes|
                  :nodesBA| nodesBA|
                  :infohashes-from-sampling| infohashes-from-sampling|
                  :infohashes-from-listening| infohashes-from-listening|
                  :infohashes-from-sybil| infohashes-from-sybil|
                  :infohashes-from-sampling|mult infohashes-from-sampling|mult
                  :infohashes-from-listening|mult infohashes-from-listening|mult
                  :infohashes-from-sybil|mult infohashes-from-sybil|mult
                  :sybils| sybils|
                  :send-krpc-request send-krpc-request
                  :count-torrentsA (atom 0)
                  :count-infohashes-from-samplingA (atom 0)
                  :count-infohashes-from-listeningA (atom 0)
                  :count-infohashes-from-sybilA (atom 0)
                  :count-discoveryA (atom 0)
                  :count-discovery-activeA (atom 0)
                  :count-messagesA count-messagesA
                  :count-messages-sybilA (atom 0)})

        state @stateA]

    (let [jframe (JFrame. "i am find program")]
      (doto jframe
        (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
        (.setSize 1600 1200)
        (.setLocationByPlatform true)
        (.setVisible true)))

    #_(go

        (println :self-id self-id)

        (find.bittorrent-dht/process-routing-table
         (merge state {:routing-table-max-size 128}))


        (find.bittorrent-dht/process-dht-keyspace
         (merge state {:routing-table-max-size 128}))

        (<! (onto-chan! nodes-to-sample|
                        (->> (:routing-table @stateA)
                             (vec)
                             (shuffle)
                             (take 8)
                             (map second))
                        false))

        (swap! stateA merge {:torrent| (let [out| (chan (sliding-buffer 100))
                                             torrent|tap (tap torrent|mult (chan (sliding-buffer 100)))]
                                         (go
                                           (loop []
                                             (when-let [value (<! torrent|tap)]
                                               (offer! out| value)
                                               (recur))))
                                         out|)})

        #_(go
            (<! (timeout duration))
            (stop))

      ; socket
        (find.bittorrent-dht/process-socket state)

      ; save state to file periodically
        (go
          (when-not (find.fs/path-exists? state-filepath)
            (<! (find.seed/write-state-file state-filepath @stateA)))
          (loop []
            (<! (timeout (* 4.5 1000)))
            (<! (find.seed/write-state-file state-filepath @stateA))
            (recur)))


      ; print info
        (let [stop| (chan 1)]
          (swap! procsA conj stop|)
          (process-print-info (merge state {:stop| stop|})))

      ; count
        (process-count state)


      ; after time passes, remove nodes from already-asked tables so they can be queried again
        (let [stop| (chan 1)]
          (swap! procsA conj stop|)
          (go
            (loop [timeout| (timeout 0)]
              (alt!
                timeout|
                ([_]
                 (doseq [[id {:keys [timestamp]}] (:routing-table-sampled @stateA)]
                   (when (> (- (find.seed/now) timestamp) (* 5 60 1000))
                     (swap! stateA update-in [:routing-table-sampled] dissoc id)))

                 (doseq [[id {:keys [timestamp interval]}] (:routing-table-find-noded @stateA)]
                   (when (or
                          (and interval (> (find.seed/now) (+ timestamp (* interval 1000))))
                          (> (- (find.seed/now) timestamp) (* 5 60 1000)))
                     (swap! stateA update-in [:routing-table-find-noded] dissoc id)))
                 (recur (timeout (* 10 1000))))

                stop|
                (do :stop)))))

      ; very rarely ask bootstrap servers for nodes
        (let [stop| (chan 1)]
          (swap! procsA conj stop|)
          (find.bittorrent-find-nodes/process-bootstrap-query
           (merge state {:stop| stop|})))

      ; periodicaly ask nodes for new nodes
        (let [stop| (chan 1)]
          (swap! procsA conj stop|)
          (find.bittorrent-find-nodes/process-dht-query
           (merge state {:stop| stop|})))

      ; start sybil
        #_(let [stop| (chan 1)]
            (swap! procsA conj stop|)
            (find.bittorrent-sybil/process
             {:stateA stateA
              :nodes-bootstrap nodes-bootstrap
              :sybils| sybils|
              :infohash| infohashes-from-sybil|
              :stop| stop|
              :count-messages-sybilA count-messages-sybilA}))

      ; add new nodes to routing table
        (go
          (loop []
            (when-let [nodesBA (<! nodesBA|)]
              (let [nodes (find.seed/decode-nodes nodesBA)]
                (>! routing-table-nodes| nodes)
                (>! dht-keyspace-nodes| nodes)
                (<! (onto-chan! nodes-to-sample| nodes false)))
              #_(println :nodes-count (count (:routing-table @stateA)))
              (recur))))

      ; ask peers directly, politely for infohashes
        (find.bittorrent-sample-infohashes/process-sampling
         state)

      ; discovery
        (find.bittorrent-metadata/process-discovery
         (merge state
                {:infohashes-from-sampling| (tap infohashes-from-sampling|mult (chan (sliding-buffer 100000)))
                 :infohashes-from-listening| (tap infohashes-from-listening|mult (chan (sliding-buffer 100000)))
                 :infohashes-from-sybil| (tap infohashes-from-sybil|mult (chan (sliding-buffer 100000)))}))

      ; process messages
        (find.bittorrent-dht/process-messages
         state))))

(comment

  (require
   '[expanse.bytes.core :as bytes.core]
   '[find.ipfs-dht :as find.ipfs-dht]
   '[find.db :as find.db]
   :reload)

  (-main)

  (swap! stateA assoc :searchS "123")

  ;
  )

(defn process-print-info
  [{:as opts
    :keys [stateA
           data-dir
           stop|
           nodes-to-sample|
           nodes-from-sampling|
           sybils|
           count-infohashes-from-samplingA
           count-infohashes-from-listeningA
           count-infohashes-from-sybilA
           count-discoveryA
           count-discovery-activeA
           count-messagesA
           count-torrentsA
           count-messages-sybilA]}]
  (let [started-at (find.seed/now)
        filepath (find.fs/path-join data-dir "find.bittorrent-dht-crawl.log.edn")
        _ (find.fs/remove filepath)
        _ (find.fs/make-parents filepath)
        writer (find.fs/writer filepath :append true)
        countA (atom 0)
        release (fn []
                  (find.protocols/close* writer))]
    (go
      (loop []
        (alt!
          (timeout (* 5 1000))
          ([_]
           (swap! countA inc)
           (let [state @stateA
                 info [[:count @countA]
                       [:infohashes [:total (+ @count-infohashes-from-samplingA @count-infohashes-from-listeningA @count-infohashes-from-sybilA)
                                     :sampling @count-infohashes-from-samplingA
                                     :listening @count-infohashes-from-listeningA
                                     :sybil @count-infohashes-from-sybilA]]
                       [:discovery [:total @count-discoveryA
                                    :active @count-discovery-activeA]]
                       [:torrents @count-torrentsA]
                       [:nodes-to-sample| (count (find.seed/chan-buf nodes-to-sample|))
                        :nodes-from-sampling| (count (find.seed/chan-buf nodes-from-sampling|))]
                       [:messages [:dht @count-messagesA :sybil @count-messages-sybilA]]
                       [:sockets @find.bittorrent-metadata/count-socketsA]
                       [:routing-table (count (:routing-table state))]
                       [:dht-keyspace (map (fn [[id routing-table]] (count routing-table)) (:dht-keyspace state))]
                       [:routing-table-find-noded  (count (:routing-table-find-noded state))]
                       [:routing-table-sampled (count (:routing-table-sampled state))]
                       [:sybils| (str (- (find.seed/fixed-buf-size sybils|) (count (find.seed/chan-buf sybils|))) "/" (find.seed/fixed-buf-size sybils|))]
                       [:time (str (int (/ (- (find.seed/now) started-at) 1000 60)) "min")]]]
             (pprint info)
             (find.protocols/write-string* writer (with-out-str (pprint info)))
             (find.protocols/write-string* writer "\n"))
           (recur))

          stop|
          (do :stop)))
      (release))))

(defn process-count
  [{:as opts
    :keys [infohashes-from-sampling|mult
           infohashes-from-listening|mult
           infohashes-from-sybil|mult
           torrent|mult
           count-infohashes-from-samplingA
           count-infohashes-from-listeningA
           count-infohashes-from-sybilA
           count-torrentsA]}]
  (let [infohashes-from-sampling|tap (tap infohashes-from-sampling|mult (chan (sliding-buffer 100000)))
        infohashes-from-listening|tap (tap infohashes-from-listening|mult (chan (sliding-buffer 100000)))
        infohashes-from-sybil|tap (tap infohashes-from-sybil|mult (chan (sliding-buffer 100000)))
        torrent|tap (tap torrent|mult (chan (sliding-buffer 100)))]
    (go
      (loop []
        (let [[value port] (alts! [infohashes-from-sampling|tap
                                   infohashes-from-listening|tap
                                   infohashes-from-sybil|tap
                                   torrent|tap])]
          (when value
            (condp = port
              infohashes-from-sampling|tap
              (swap! count-infohashes-from-samplingA inc)

              infohashes-from-listening|tap
              (swap! count-infohashes-from-listeningA inc)

              infohashes-from-sybil|tap
              (swap! count-infohashes-from-sybilA inc)

              torrent|tap
              (swap! count-torrentsA inc))
            (recur)))))))





(comment

  (let [data-dir (io/file (System/getProperty "user.dir") "data")]
    (println data-dir)
    (println (.getCanonicalFile data-dir))
    (println (.getAbsolutePath data-dir)))

  ;;
  )



(comment

  clj -Sdeps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}
                      org.clojure/core.async {:mvn/version "1.3.618"}
                      expanse/bytes-jvm {:local/root "./expanse/bytes-jvm"}
                      expanse/codec-jvm {:local/root "./expanse/codec-jvm"}
                      expanse/core-jvm {:local/root "./expanse/core-jvm"}
                      expanse/datagram-socket-jvm {:local/root "./expanse/datagram-socket-jvm"}
                      expanse/socket-jvm {:local/root "./expanse/socket-jvm"}
                      expanse/fs-jvm {:local/root "./expanse/fs-jvm"}
                      expanse/fs-meta {:local/root "./expanse/fs-meta"}
                      expanse/transit-jvm {:local/root "./expanse/transit-jvm"}
                      expanse.bittorrent/spec {:local/root "./bittorrent/spec"}
                      expanse.bittorrent/bencode {:local/root "./bittorrent/bencode"}
                      expanse.bittorrent/wire-protocol {:local/root "./bittorrent/wire-protocol"}
                      expanse.bittorrent/dht-crawl {:local/root "./bittorrent/dht-crawl"}}} '(require '[expanse.bittorrent.dht-crawl.core :as dht-crawl.core] :reload-all)

  clj -Sdeps '{:deps {org.clojure/clojurescript {:mvn/version "1.10.844"}
                      org.clojure/core.async {:mvn/version "1.3.618"}
                      expanse/bytes-meta {:local/root "./expanse/bytes-meta"}
                      expanse/bytes-js {:local/root "./expanse/bytes-js"}
                      expanse/codec-js {:local/root "./expanse/codec-js"}
                      expanse/core-js {:local/root "./expanse/core-js"}
                      expanse/datagram-socket-nodejs {:local/root "./expanse/datagram-socket-nodejs"}
                      expanse/fs-nodejs {:local/root "./expanse/fs-nodejs"}
                      expanse/fs-meta {:local/root "./expanse/fs-meta"}
                      expanse/socket-nodejs {:local/root "./expanse/socket-nodejs"}
                      expanse/transit-js {:local/root "./expanse/transit-js"}

                      expanse.bittorrent/spec {:local/root "./bittorrent/spec"}
                      expanse.bittorrent/bencode {:local/root "./bittorrent/bencode"}
                      expanse.bittorrent/wire-protocol {:local/root "./bittorrent/wire-protocol"}
                      expanse.bittorrent/dht-crawl {:local/root "./bittorrent/dht-crawl"}}} '\
  -M -m cljs.main \
  -co '{:npm-deps {"randombytes" "2.1.0"
                   "bitfield" "4.0.0"
                   "fs-extra" "9.1.0"}
        :install-deps true
        :analyze-path "./bittorrent/dht-crawl"
        :repl-requires [[cljs.repl :refer-macros [source doc find-doc apropos dir pst]]
                        [cljs.pprint :refer [pprint] :refer-macros [pp]]]} '\
  -ro '{:host "0.0.0.0"
        :port 8899} '\
  --repl-env node --compile expanse.bittorrent.dht-crawl.core --repl

  (require
   '[clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                      pub sub unsub mult tap untap mix admix unmix pipe
                                      timeout to-chan  sliding-buffer dropping-buffer
                                      pipeline pipeline-async]]
   '[clojure.core.async.impl.protocols :refer [closed?]])

  (require
   '[find.fs]
   '[find.bytes]
   '[find.codec]
   '[find.bencode]
   '[expanse.bittorrent.dht-crawl.core :as dht-crawl.core]
   :reload #_:reload-all)

  (dht-crawl.core/start
   {:data-dir (find.fs/path-join "./dht-crawl")})




  ;
  )



(comment

  (let [c| (chan 10 (map (fn [value]
                           (println [:mapping (.. (Thread/currentThread) (getName))])
                           (inc value))))]

    (go
      (loop [i 10]
        (when (> i 0)
          (<! (timeout 1000))
          (>! c| i)
          (recur (dec i))))
      (close! c|)
      (println [:exit 0]))

    (go
      (loop []
        (when-let [value (<! c|)]
          (println [:took value (.. (Thread/currentThread) (getName))])
          (recur)))
      (println [:exit 1]))

    (go
      (loop []
        (when-let [value (<! c|)]
          (println [:took value (.. (Thread/currentThread) (getName))])
          (recur)))
      (println [:exit 2]))

    (go
      (loop []
        (when-let [value (<! c|)]
          (println [:took value (.. (Thread/currentThread) (getName))])
          (recur)))
      (println [:exit 3]))

    (go
      (loop []
        (when-let [value (<! c|)]
          (println [:took value (.. (Thread/currentThread) (getName))])
          (recur)))
      (println [:exit 4])))

  ;
  )



(comment

  (let [stateA (atom (transient {}))]
    (dotimes [n 8]
      (go
        (loop [i 100]
          (when (> i 0)
            (get @stateA :n)
            (swap! stateA dissoc! :n)
            (swap! stateA assoc! :n i)


            (recur (dec i))))
        (println :done n))))

  ; java.lang.ArrayIndexOutOfBoundsException: Index -2 out of bounds for length 16
  ; at clojure.lang.PersistentArrayMap$TransientArrayMap.doWithout (PersistentArrayMap.java:432)
  ; at clojure.lang.ATransientMap.without (ATransientMap.java:69)
  ; at clojure.core$dissoc_BANG_.invokeStatic (core.clj:3373)


  (time
   (loop [i 10000000
          m (transient {})]
     (if (> i 0)

       (recur (dec i) (-> m
                          (assoc! :a 1)
                          (dissoc! :a)
                          (assoc! :a 2)))
       (persistent! m))))

  ; "Elapsed time: 799.025172 msecs"

  (time
   (loop [i 10000000
          m {}]
     (if (> i 0)

       (recur (dec i) (-> m
                          (assoc :a 1)
                          (dissoc :a)
                          (assoc :a 2)))
       m)))

  ; "Elapsed time: 1361.090409 msecs"

  (time
   (loop [i 10000000
          m (sorted-map)]
     (if (> i 0)

       (recur (dec i) (-> m
                          (assoc :a 1)
                          (dissoc :a)
                          (assoc :a 2)))
       m)))

  ; "Elapsed time: 1847.529152 msecs"

  ;
  )
