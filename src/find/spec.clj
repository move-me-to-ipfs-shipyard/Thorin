(ns find.spec
  (:require
   [clojure.spec.alpha :as s]
   [find.protocols :as find.protocols]))


(s/def ::searchS string?)


(s/def ::host string?)
(s/def ::port int?)

(s/def ::channel #?(:clj #(instance? clojure.core.async.impl.channels.ManyToManyChannel %)
                    :cljs #(instance? cljs.core.async.impl.channels/ManyToManyChannel %)))

(s/def ::msg| ::channel)
(s/def ::evt| ::channel)
(s/def ::ex| ::channel)

(s/def ::datagram-socket #(and
                           (satisfies? find.protocols/DatagramSocket %)
                           #?(:clj (instance? clojure.lang.IDeref %))
                           #?(:cljs (satisfies? cljs.core/IDeref %))))


(s/def ::connection| ::channel)

(s/def ::time-out int?)

(s/def ::socket #(and
                  (satisfies? find.protocols/Socket %)
                  (satisfies? find.protocols/Close %)
                  #?(:clj (instance? clojure.lang.IDeref %))
                  #?(:cljs (satisfies? cljs.core/IDeref %))))


(s/def ::infohash string?)
(s/def ::infohashBA ::bytes.spec/byte-array)

(s/def ::peer-id string?)
(s/def ::peer-idBA ::bytes.spec/byte-array)

(s/def ::wire #(and
                (satisfies? bittorrent.protocols/Wire %)
                #?(:clj (instance? clojure.lang.IDeref %))
                #?(:cljs (satisfies? cljs.core/IDeref %))))

(s/def ::channel #?(:clj #(instance? clojure.core.async.impl.channels.ManyToManyChannel %)
                    :cljs #(instance? cljs.core.async.impl.channels/ManyToManyChannel %)))

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