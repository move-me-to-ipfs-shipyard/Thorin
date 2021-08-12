(ns find.codec
  (:import
   (org.apache.commons.codec.binary Hex)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defn hex-to-bytes ^bytes
  [^String string]
  (Hex/decodeHex string))

(defn hex-to-string ^String
  [^bytes byte-arr]
  (Hex/encodeHexString byte-arr))

(comment
  
  clj -Sdeps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}
                      org.clojure/core.async {:mvn/version "1.3.618"}
                      expanse/codec-jvm {:local/root "./expanse/codec-jvm"}
                      expanse/bytes {:local/root "./expanse/bytes-jvm"}
                      }}'
  
  (do
    (require '[clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close! onto-chan!
                                                pub sub unsub mult tap untap mix admix unmix pipe
                                                timeout to-chan  sliding-buffer dropping-buffer
                                                pipeline pipeline-async]])
    (require '[expanse.codec.core :as codec.core])
    (require '[find.bytes])
    ))
  
  
  ;
  )