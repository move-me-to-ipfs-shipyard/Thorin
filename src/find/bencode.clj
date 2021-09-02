(ns find.bencode
  (:require 
    [clojure.core.async :as a :refer [<! >! <!! >!! chan put! take! go alt! alts! do-alts close! timeout pipe mult tap untap 
                                      pub sub unsub mix admix unmix dropping-buffer sliding-buffer pipeline pipeline-async to-chan! thread]]
    [clojure.string]
    [clojure.java.io :as io])
)

(def i-int (int \i))
(def e-int (int \e))
(def d-int (int \d))
(def l-int (int \d))
(def colon-int (int \:))