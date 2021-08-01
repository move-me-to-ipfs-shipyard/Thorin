(ns find.spec
  (:require
   [clojure.spec.alpha :as s]
   [find.protocols :as find.protocols])
  (:import
   (java.nio ByteBuffer)))

(s/def ::byte-array bytes?)
(s/def ::byte-buffer #(instance? java.nio.ByteBuffer %))

(s/def ::searchS string?)


(s/def ::host string?)
(s/def ::port int?)

(s/def ::channel #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))

(s/def ::msg| ::channel)
(s/def ::evt| ::channel)
(s/def ::ex| ::channel)

(s/def ::datagram-socket #(and
                           (satisfies? find.protocols/DatagramSocket %)
                           (instance? clojure.lang.IDeref %)))


(s/def ::connection| ::channel)

(s/def ::time-out int?)

(s/def ::socket #(and
                  (satisfies? find.protocols/Socket %)
                  (satisfies? find.protocols/Close %)
                  (instance? clojure.lang.IDeref %)))


(s/def ::infohash string?)
(s/def ::infohashBA ::byte-array)

(s/def ::peer-id string?)
(s/def ::peer-idBA ::byte-array)

(s/def ::wire #(and
                (satisfies? find.protocols/Wire %)
                (instance? clojure.lang.IDeref %)))

(s/def ::channel #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))

(s/def ::recv| ::channel)
(s/def ::send| ::channel)
(s/def ::ex| ::channel)
(s/def ::metadata| ::channel)

(s/def ::create-wire-opts
  (s/keys :req [::send|
                ::recv|
                ::metadata|
                ::infohashBA
                ::peer-idBA]
          :opt [::ex|]))