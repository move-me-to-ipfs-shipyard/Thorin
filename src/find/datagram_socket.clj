(ns find.datagram-socket
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]

   [find.bytes]
   [find.protocols])
  (:import
   (java.net InetSocketAddress InetAddress)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defn create
  [{:as opts
    :keys [:host
           :port
           :evt|
           :msg|
           :ex|]
    :or {host "0.0.0.0"
         port 6881}}]
  (let []))


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