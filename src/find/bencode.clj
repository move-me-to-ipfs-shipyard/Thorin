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
        (map? value) :dictionary
        (sequential? value) :list
        (int? value) :integer
      )
    )
    ([value baos dispatch-value] dispatch-value)
  )
)

(defmethod encode* :byte-arr
  [^bytes value ^ByteArrayOutputStream baos & args]
  (.writeBytes baos (-> (alength value) (str) (.getBytes "UTF-8")))
  (.write baos colon-int)
  (.writeBytes baos value)
)

(defmethod encode* :string
  [^String value ^ByteArrayOutputStream baos & args]
  (encode* baos (.getBytes value "UTF-8") :byte-arr)
)

(defmethod encode* :integer
  [^int value ^ByteArrayOutputStream baos & args]
  (.write baos i-int)
  (.writeBytes baos (-> value (str) (.getBytes "UTF-8")))
  (.write baos e-int)
)

(defmethod encode* :dictionary
  [value ^ByteArrayOutputStream baos & args]
  (.write baos d-int)
  (doseq [[k v] value]
    (encode* (str k) baos)
    (encode* v baos)
  )
  (.write baos e-int)
)

(defmethod encode* :list
  [value ^ByteArrayOutputStream baos & args]
  (.write baos l-int)
  (doseq [v value]
    (encode* v baos)
  )
  (.write baos e-int)
)

(defn encode ^bytes
  [data]
  (let [baos (ByteArrayOutputStream.)]
    (encode* data baos)
    (.toByteArray baos)
  )
)

(defn read-until ^bytes
   [^PushbackInputStream pbis ^int target-byte]
  (let [baos (ByteArrayOutputStream.)]
    (loop [byte (.read pbis)]
      (cond
        (== byte target-byte)
        (.toByteArray baos)

        :else
        (let []
          (.write baos byte)
          (recur (.read pbis))
        )
      )
    )
  )
)

(defmulti decode*
  (fn 
    ([byte-arr ^PushbackInputStream pbis]
      (let [byte (.read pbis)]
        (.unread pbis byte)
        (condp == byte
          -1 (throw (ex-info "input stream ends unexpectedly" {}))
          d-int :dictionary
          i-int :integer
          l-int :list
          :else :byte-arr
        )
      )
    )
    ([byte-arr pbis dispatch-value] dispatch-value)
  )
)

(defmethod decode* :byte-arr ^bytes
  [^PushbackInputStream pbis & args]
  (let [sizeBA (read-until pbis colon-int)
        size (Integer/parseInt (String. sizeBA "UTF-8"))
        dataBA (.readNBytes pbis size)]
    dataBA
  )
)

(defmethod decode* :integer ^int
  [^PushbackInputStream pbis & args]
  (.read pbis)
  (let [byte-arr (read-until pbis e-int)]
    (.read pbis)
    (Integer/parseInt (String. byte-arr "UTF-8"))
  )
)

(defmethod decode* :dictionary
  [^PushbackInputStream pbis & args]
  (let []
    (.read pbis)
    (loop [resultT (transient [])]
      (let [byte (.read pbis)]
        (.unread pbis)
        (cond
          (odd? (count resultT))
          (recur (conj! resultT (decode* pbis :string))) 
          
          (== byte i-int)
          (recur (conj! resultT (decode* pbis :integer)))

          (== byte d-int)
          (recur (conj! resultT (decode* pbis :dictionary)))

          (== byte l-int)
          (recur (conj! resultT (decode* pbis :list)))

          (== byte e-int)
          (let []
            (.read pbis)
            (apply array-map (persistent! resultT))
          )
        )
      )
    )
  )
)

(defmethod decode* :list
  [^PushbackInputStream pbis & args]
  (let []
    (.read pbis)
    (loop [resultT (transient [])]
      (let [byte (.read pbis)]
        (.unread pbis)
        (cond
          
          (== byte i-int)
          (recur (conj! resultT (decode* pbis :integer)))

          (== byte d-int)
          (recur (conj! resultT (decode* pbis :dictionary)))

          (== byte l-int)
          (recur (conj! resultT (decode* pbis :list)))

          (== byte e-int)
          (let []
            (.read pbis)
            (persistent! resultT)
          )

          :else
          (recur (conj! resultT (decode* pbis :string))) 
        )
      )
    )
  )
)

(defn decode
  [^bytes byte-arr]
  (let [pbis (-> 
              (ByteArrayInputStream. byte-arr)
              (PushbackInputStream.)
              ) ]
    (decode* pbis)
  )
)



(comment



)