(ns find.protocols)

(defprotocol DatagramSocket
  (close* [_])
  (listen* [_])
  (send* [_ data address])
  #_IDeref)


(defprotocol Close
  (close* [_]))

(defprotocol Socket
  (connect* [_])
  (send* [_ data])
  #_Close
  #_IDeref)