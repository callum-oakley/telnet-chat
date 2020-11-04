(ns bus
  (:require [clojure.core.async :as async]))

;; Contains a map from channel name to a set of subscribers (async/chan),
;; wrapped in a ref so we can safely change it across threads.
(defn bus []
  (ref {}))

(defn pub [bus chan msg]
  (doseq [subscriber (@bus chan)]
    (async/put! subscriber {:type :msg
                            :chan chan
                            :msg msg})))

(defn sub [bus chan subscriber]
  (dosync
    (alter bus update chan #(into #{subscriber} %))))

(defn unsub [bus chan subscriber]
  (dosync
    (alter bus update chan disj subscriber)))

;;;; TODO core.async actually has a dedicated publish subscribe model which I
;;;; should probably be using instead.
