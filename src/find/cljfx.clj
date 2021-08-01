(ns find.cljfx
  (:gen-class)
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >! <!! >!!  take! put! offer! poll! alt! alts! close! onto-chan!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.string]
   [clojure.java.io :as io]
   [cljfx.api]
   [find.spec :as find.spec])
  (:import
   (javafx.event Event EventHandler)
   (javafx.stage WindowEvent)
   (javafx.scene.control DialogEvent Dialog ButtonType ButtonBar$ButtonData)
   (javafx.application Platform)))

(set! *warn-on-reflection* true)

(defn root
  [{:as state
    :keys [::find.spec/searchS]}]
  {:fx/type :stage
   :showing true
   #_:on-close-request #_(fn [^WindowEvent event]
                           (println :on-close-request)
                           #_(.consume event))
   :icons [(str (io/resource "logo-728.png"))]
   :width 1024
   :height 768
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :children [{:fx/type :label :text "find"}
                             {:fx/type :text-field
                              :text searchS}]}}})

(def renderer (cljfx.api/create-renderer))

(defn process
  [{:as opts
    :keys [stateA]}]
  (Platform/setImplicitExit true)
  (add-watch stateA :renderer (fn [k stateA old-state new-state] (find.cljfx/renderer new-state)))
  (renderer @stateA)
  #_(cljfx.api/mount-renderer stateA render))