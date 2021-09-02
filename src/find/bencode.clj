(ns find.bencode
  (:require 
    [clojure.core.async :as a :refer [<! >! <!! >!! chan put! take! go alt! alts! do-alts close! timeout pipe mult tap untap 
                                      pub sub unsub mix admix unmix dropping-buffer sliding-buffer pipeline pipeline-async to-chan! thread]]
    [clojure.string]
    [clojure.java.io :as io]
  )
  (:import
    (java.io ByteArrayOutputStream ByteArrayInputStream PushbackInputStream)
  )
)

(def i-int (int \i))
(def e-int (int \e))
(def d-int (int \d))
(def l-int (int \d))
(def colon-int (int \:))

(defmulti encode* 
  (fn 
    ([value baos] 
      (cond
        (bytes? value) :byte-arr
        (string? value) :string
        (map? value) :map
        (sequential? value) :list
        (int? value) :int
      )
    )
    ([value baos dispatch-value] dispatch-value)
  )
)

(defn encode
  [data]
  (let [baos (ByteArrayOutputStream.)]
    (encode* data baos)
  )
)

(defmulti decode*
  (fn [byte-arr bais] )
)

(defn decode
  [byte-arr]
  (let [bais (-> 
              (ByteArrayInputStream. byte-arr)
              (PushbackInputStream.)
              ) ]
    (decode* byte-arr bais)
  )
)



(comment



)