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

(defprotocol IPushbackInputStream
  (read* [_] [_ length])
  (unread* [_ char-int])
  (unread-byte-array* [_ byte-arr] [_ byte-arr offset length]))

(defprotocol IToByteArray
  (to-byte-array* [_]))

(defprotocol IByteArrayOutputStream
  (write* [_ char-int])
  (write-byte-array* [_ byte-arr])
  (reset* [_])
  #_IToByteArray)

(defprotocol Closable
  (close [_]))