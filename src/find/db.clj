(ns find.db
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >! <!! >!!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.java.io :as io]
   [find.bytes]
   [find.codec]
   [find.spec :as find.spec]
   [datahike.api]
   [taoensso.timbre :as log]
   [taoensso.timbre.appenders.3rd-party.rotor]))

(log/merge-config! {:level :debug
                    :min-level :info
                    :appenders {:rotating (taoensso.timbre.appenders.3rd-party.rotor/rotor-appender
                                           {:path
                                            (let [peer-index 1]
                                              (->
                                               (io/file (System/getProperty "user.dir") "volumes" (format "peer%s" peer-index) "db-server.log")
                                               (.getCanonicalFile)))
                                            :max-size (* 512 1024)
                                            :backlog 10})}})

(comment

  (require
   '[find.db :as find.db]
   '[find.bytes]
   '[find.codec]
   '[find.spec :as find.spec]
   '[datahike.api]
   :reload)

  (do
    (let [peer-index 1]
      (io/make-parents (System/getProperty "user.dir") "volumes" (format "peer%s" peer-index) "db"))
    (def cfg {:store {:backend :file
                      :path "./volumes/peer1/db"}})
    (datahike.api/create-database cfg)
    (def conn (datahike.api/connect cfg)))

  (do
    (datahike.api/release conn)
    (datahike.api/delete-database cfg))

  (datahike.api/transact conn [{:db/ident :name
                                :db/valueType :db.type/string
                                :db/cardinality :db.cardinality/one}
                               {:db/ident :age
                                :db/valueType :db.type/long
                                :db/cardinality :db.cardinality/one}])

  (datahike.api/transact conn [{:name  "Alice", :age   20}
                               {:name  "Bob", :age   30}
                               {:name  "Charlie", :age   40}
                               {:age 15}])

  (datahike.api/q '[:find ?e ?n ?a
                    :where
                    [?e :name ?n]
                    [?e :age ?a]]
                  @conn)

  (datahike.api/transact conn {:tx-data [{:db/id 3 :age 25}]})

  (datahike.api/q {:query '{:find [?e ?n ?a]
                            :where [[?e :name ?n]
                                    [?e :age ?a]]}
                   :args [@conn]})

  (datahike.api/q '[:find ?a
                    :where
                    [?e :name "Alice"]
                    [?e :age ?a]]
                  (datahike.api/history @conn))

  ;
  )

(comment

  (datahike.api/transact conn [{:db/ident ::find.spec/infohash
                                :db/valueType :db.type/string
                                :db/cardinality :db.cardinality/one}
                               {:db/ident ::find.spec/metadata
                                :db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/one}
                               {:db/ident ::find.spec/name
                                :db/valueType :db.type/string
                                :db/cardinality :db.cardinality/one}
                               {:db/ident ::find.spec/files
                                :db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}])

  (time
   (dotimes [n 50]
     (when (zero? (mod n 10))
       (println :n n))
     (datahike.api/transact conn (map
                                  (fn [i]
                                    {::find.spec/infohash (-> (find.bytes/random-bytes 20) (find.codec/hex-to-string))
                                     ::find.spec/metadata
                                     {::find.spec/name (str "some torrent name details 12312312 foo bar " n "," i)
                                      ::find.spec/files (map (fn [j]
                                                                     {::find.spec/name (str "some file name details 12312312 foo bar " j)}) (range 0 10))}})
                                  (range 100)))
     nil))

  (->
   (datahike.api/q '[:find ?e ?infohash
                     :where
                     [?e ::find.spec/infohash ?infohash]]
                   @conn)
   (count))
  
  ; 5 txn by 1000 items "Elapsed time: 93662.345254 msecs"
  ; 50 txn by 100 items "Elapsed time: 106989.965732 msecs"



  ;
  )

