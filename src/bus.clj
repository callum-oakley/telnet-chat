(ns bus)

(defprotocol Subscriber
  "Represents a subscriber to the bus which can have messages delivered to it"
  (post [this data]))

;; Contains a map from channel name to a set of subscribers, wrapped in an atom
;; so we can safely change it across threads.
(defn bus []
  (atom {}))

(defn pub [bus chan from msg]
  (doseq [subscriber (@bus chan)]
    (post subscriber {:type :msg
                      :chan chan
                      :from from
                      :msg msg})))

(defn sub [bus chan subscriber]
  (swap! bus update chan #(into #{subscriber} %)))

(defn unsub [bus chan subscriber]
  (swap! bus update chan disj subscriber))

(defn update-all
  "Like update, but applies f to every value in the map"
  [m f & args]
  (reduce (fn [acc [k v]] (assoc acc k (apply f v args))) {} m))

(defn unsub-all [bus subscriber]
  (swap! bus update-all disj subscriber))

;;;; TODO core.async actually has a dedicated publish subscribe model which I
;;;; should probably be using instead, this was a good excuse to play with
;;;; atoms and map updates though...
