(ns mee6.util.timers
  (:require [beicon.core :as rx]))

(defn schedule
  ([func] (schedule 0 func))
  ([ms func]
   (let [sem (js/setTimeout #(func) ms)]
     (reify rx/ICancellable
       (-cancel [_]
         (js/clearTimeout sem))))))

(defn interval
  [ms func]
  (let [sem (js/setInterval #(func) ms)]
    (reify rx/ICancellable
      (-cancel [_]
        (js/clearInterval sem)))))
