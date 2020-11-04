(ns server
  (:require
    [clojure.java.io :as io]
    [clojure.core.async :as async :refer [<! >!]]
    [request]
    [bus])
  (:import [java.net ServerSocket]))

(defn write [writer s]
  (.write writer s)
  (.flush writer))

;; TODO gracefully handle closed sockets and unsubscribe from the bus
(defn read-loop [bus subscriber reader]
  (async/go-loop [] ; TODO should this be thread since we're doing blocking io?
    (>! subscriber {:type :prompt})
    (let [req (request/parse (.readLine reader))]
      (println "parsed request:" req)
      (case (:type req)
        :pub (do
               (>! subscriber {:type :ok})
               (bus/pub bus (:chan req) (:msg req)))
        :sub (do
               (>! subscriber {:type :ok})
               (bus/sub bus (:chan req) subscriber))
        :unsub (do
                 (>! subscriber {:type :ok})
                 (bus/unsub bus (:chan req) subscriber))
        :err (>! subscriber {:type :err
                             :err (:err req)})))
    (recur)))

;; Serialize writes through the subscriber channel
(defn write-loop [subscriber writer]
  (async/go-loop []
    (let [m (<! subscriber)]
      (case (:type m)
        :prompt (write writer "> ")
        :ok (write writer "OK\n")
        :msg (write writer (format "MSG %s %s\n" (:chan m) (:msg m)))
        :err (write writer (format "ERR %s\n" (:err m)))))
    (recur)))

;; Blocks forever
(defn serve [bus port]
  (with-open [server-sock (ServerSocket. port)]
    (println (str "listening on :" port))
    (while true
      (let [sock (.accept server-sock)
            subscriber (async/chan)]
        (read-loop bus subscriber (io/reader sock))
        (write-loop subscriber (io/writer sock))))))
