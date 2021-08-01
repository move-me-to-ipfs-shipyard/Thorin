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
   [find.protocols]

   [manifold.deferred :as d]
   [manifold.stream :as sm]
   [aleph.tcp])
  (:import
   (java.net InetSocketAddress)
   (io.netty.bootstrap Bootstrap)
   (io.netty.channel ChannelPipeline)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(s/def ::ssl-handler #(instance? io.netty.handler.ssl.SslHandler %))

(s/def ::socket-stream #(instance? manifold.stream.SplicedStream %))

(s/def ::opts (s/keys :req [::find.spec/evt|
                            ::find.spec/msg|
                            ::find.spec/ex|]
                      :opt [::find.spec/port
                            ::find.spec/host
                            ::socket-stream
                            ::ssl-handler]))

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
  (let [streamV (volatile! nil)
        socket
        ^{:type ::find.spec/socket}
        (reify
          find.protocols/Socket
          (connect*
            [t]
            (->
             (d/chain
              (if socket-stream
                socket-stream
                (aleph.tcp/client (merge
                                   {:host host
                                    :port port
                                    :insecure? true}
                                   opts)))
              (fn [stream]
                (vreset! streamV stream)
                (put! evt| {:op :connected})
                stream)
              (fn [stream]
                (d/loop []
                  (->
                   (sm/take! stream nil)
                   (d/chain
                    (fn [byte-arr]
                      (if byte-arr
                        (do
                          (put! msg| byte-arr)
                          (d/recur))
                        (do
                          (when @streamV
                            (throw (ex-info (str ::socket-stream-closed) {} nil)))))))
                   (d/catch Exception (fn [ex]
                                        (put! ex| ex)
                                        (find.protocols/close* t)))))))
             (d/catch Exception (fn [ex]
                                  (put! ex| ex)
                                  (find.protocols/close* t)))))
          find.protocols/Send
          (send*
            [_ byte-arr]
            (sm/put! @streamV byte-arr))
          find.protocols/Close
          (close*
            [_]
            (when-let [stream @streamV]
              (vreset! streamV nil)
              (sm/close! stream)))
          clojure.lang.IDeref
          (deref [_] @streamV))]

    socket))


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
    (require '[manifold.deferred :as d])
    (require '[manifold.stream :as sm]))


  (def s (sm/stream))
  (sm/consume #(prn %) s)
  (sm/put! s 1)

  ;
  )