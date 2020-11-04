(ns bus)

(defprotocol Subscriber
  "Represents a subscriber to the bus which can have messages delivered to it"
  (post [this data]))

;; Contains a map from channel name to a set of subscribers (async/chan),
;; wrapped in an atom so we can safely change it across threads.
(defn bus []
  (atom {}))

(defn pub [bus chan msg]
  (doseq [subscriber (@bus chan)]
    (post subscriber {:type :msg
                      :chan chan
                      :msg msg})))

(defn sub [bus chan subscriber]
  (swap! bus update chan #(into #{subscriber} %)))

(defn unsub [bus chan subscriber]
  (swap! bus update chan disj subscriber))

;;;; TODO core.async actually has a dedicated publish subscribe model which I
;;;; should probably be using instead, this was a good excuse to play with
;;;; atoms though...
