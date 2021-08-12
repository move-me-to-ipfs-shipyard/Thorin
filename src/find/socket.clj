(ns find.socket
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   [clojure.spec.alpha :as s]

   [find.bytes]
   [find.spec :as find.spec]
   [find.protocols])
  (:import
   (java.net InetSocketAddress)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(s/def ::opts (s/keys :req [::find.spec/evt|
                            ::find.spec/msg|
                            ::find.spec/ex|]
                      :opt [::find.spec/port
                            ::find.spec/host]))

(defn create
  [{:as opts
    :keys [::find.spec/port
           ::find.spec/host
           ::socket-stream
           ::find.spec/evt|
           ::find.spec/msg|
           ::find.spec/ex|]}]
  {:pre [(s/assert ::opts opts)]
   :post [(s/assert ::find.spec/socket %)]}
  (let []))


(comment


  clj -Sdeps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}
                      org.clojure/core.async {:mvn/version "1.3.618"}
                      expanse/bytes-jvm {:local/root "./expanse/bytes-jvm"}
                      expanse/bytes-meta {:local/root "./expanse/bytes-meta"}
                      expanse/socket-jvm {:local/root "./expanse/socket-jvm"}}}'
  
  (do
    (require '[clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                                pub sub unsub mult tap untap mix admix unmix pipe
                                                timeout to-chan  sliding-buffer dropping-buffer
                                                pipeline pipeline-async]])
    (require '[expanse.socket.core :as socket.core])


  ;
  )