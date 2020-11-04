(ns user
  (:require
    [clojure.core.async :as async]
    [bus]))

;; An implementation of bus/Subscriber which uses an async/chan as an inbox
(defrecord User [inbox nick]
  bus/Subscriber
  (post [{inbox :inbox} data]
    (async/put! inbox data)))

(def nick-suffix
  (atom 0))

(defn user
  "Creates a user with a new inbox channel and gives them unique nickname of the form @userN for some integer N"
  []
  (->User (async/chan) (atom (str "@user" (swap! nick-suffix inc)))))

(defn nick [user]
  @(:nick user))

(defn identify [user nick]
  (reset! (:nick user) nick))

;;;; TODO ensure custom nicknames are unique
