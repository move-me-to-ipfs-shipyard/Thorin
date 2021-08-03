(ns find.datagram-socket
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   [clojure.spec.alpha :as s]

   [find.bytes]
   [find.spec :as find.spec]
   [find.protocols]
   [manifold.deferred :as d]
   [manifold.stream :as sm]
   [aleph.udp])
  (:import
   (java.net InetSocketAddress InetAddress)
   (io.netty.bootstrap Bootstrap)
   (io.netty.channel ChannelPipeline)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(s/def ::opts (s/keys :req [::find.spec/host
                            ::find.spec/port
                            ::find.spec/evt|
                            ::find.spec/msg|
                            ::find.spec/ex|]
                      :opt []))

(defn create
  [{:as opts
    :keys [::find.spec/host
           ::find.spec/port
           ::find.spec/evt|
           ::find.spec/msg|
           ::find.spec/ex|]
    :or {host "0.0.0.0"
         port 6881}}]
  {:pre [(s/assert ::opts opts)]
   :post [(s/assert ::find.spec/datagram-socket %)]}
  (let [streamV (volatile! nil)

        socket
        ^{:type ::find.spec/datagram-socket}
        (reify
          find.protocols/DatagramSocket
          (listen*
            [t]
            (->
             (d/chain
              (aleph.udp/socket {:socket-address (InetSocketAddress. ^String host ^int port)
                                 :insecure? true})
              (fn [stream]
                (vreset! streamV stream)
                (put! evt| {:op :listening})
                stream)
              (fn [stream]
                (d/loop []
                  (->
                   (sm/take! stream ::none)
                   (d/chain
                    (fn [msg]
                      (when-not (identical? msg ::none)
                        (let [^InetSocketAddress inet-socket-address (:sender msg)]
                          #_[^InetAddress inet-address (.getAddress inet-socket-address)]
                          #_(.getHostAddress inet-address)
                          (put! msg| {:msgBA (:message msg)
                                      :host (.getHostString inet-socket-address)
                                      :port (.getPort inet-socket-address)}))
                        (d/recur))))
                   (d/catch Exception (fn [ex]
                                        (put! ex| ex)
                                        (find.protocols/close* t)))))))
             (d/catch Exception (fn [ex]
                                  (put! ex| ex)
                                  (find.protocols/close* t)))))
          find.protocols/Send
          (send*
            [_ byte-arr {:keys [host port]}]
            (sm/put! @streamV {:host host
                               :port port
                               :message byte-arr}))
          find.protocols/Close
          (close*
            [_]
            (when-let [stream @streamV]
              (sm/close! stream)))
          clojure.lang.IDeref
          (deref [_] @streamV))]

    socket))


(comment
  
  
  clj -Sdeps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}
                      org.clojure/core.async {:mvn/version "1.3.618"}
                      expanse/bytes-jvm {:local/root "./expanse/bytes-jvm"}
                      expanse/bytes-meta {:local/root "./expanse/bytes-meta"}
                      expanse/datagram-socket-jvm {:local/root "./expanse/datagram-socket-jvm"}}}'
  
  (do
    (require '[clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                                pub sub unsub mult tap untap mix admix unmix pipe
                                                timeout to-chan  sliding-buffer dropping-buffer
                                                pipeline pipeline-async]])
    (require '[expanse.datagram-socket.core :as datagram-socket.core]))
  
  (do
    (def c| (chan 10))
    (go (loop []
          (when-let [v (<! c|)]
            (println :c v)
            (recur))))
    (->
     (a/thread
       (try
         (loop [i 3]
           (when (= i 0)
             (throw (ex-info "error" {})))
           (Thread/sleep 1000)
           (println i)
           (put! c| i)
           (recur (dec i)))
         (catch Exception e
           :foo)))
     (take! prn)))
  
  ;
  )