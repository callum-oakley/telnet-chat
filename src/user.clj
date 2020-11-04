(ns user
  (:require
    [clojure.core.async :as async]
    [bus]))

;; An implementation of bus/Subscriber which uses an async/chan as an inbox
(defrecord User [inbox]
  bus/Subscriber
  (post [{inbox :inbox} data]
    (async/put! inbox data)))

(defn user []
  (->User (async/chan)))
