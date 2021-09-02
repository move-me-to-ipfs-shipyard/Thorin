(ns find.main
  (:gen-class)
  (:require 
    [clojure.core.async :as a :refer [<! >! <!! >!! chan put! take! go alt! alts! do-alts close! timeout pipe mult tap untap 
                                      pub sub unsub mix admix unmix dropping-buffer sliding-buffer pipeline pipeline-async to-chan! thread]]
    [clojure.string]
    [clojure.java.io :as io]
    
    [find.seed]
    [find.datagram]
    [find.bencode]
    )
  (:import
    (javax.swing JFrame WindowConstants ImageIcon JTextField JPanel)
  )    
)

#_(println (System/getProperty "clojure.core.async.pool-size"))
(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defonce stateA (atom nil))
(def ^:dynamic jframe nil)
(def ^:dynamic input nil)

(defn window
  []
  (let [jframe (JFrame. "i am find program")
        input (JTextField. 100)
        jpanel (JPanel.)]

  (when-let [url (io/resource "icon.png")]
    (.setIconImage jframe (.getImage (ImageIcon. url)))
  )

  (doto jpanel
    (.add input)
  )

  (doto jframe
    (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
    (.setSize 1600 1200)
    (.setLocation 1700 300)
    (.add jpanel)
    (.setVisible true)
  )

  (alter-var-root #'find.main/jframe (constantly jframe))

  nil
  )
)

(defn reload
  []
  (require 
    '[find.main]
    :reload)
)

(defn -main
  [& args]
  (let []
    (reset! stateA {})

    (window)

  )
)
