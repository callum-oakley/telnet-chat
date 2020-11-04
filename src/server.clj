(ns server
  (:require
    [clojure.java.io :as io]
    [clojure.core.async :as async :refer [<!]]
    [request]
    [user]
    [bus])
  (:import [java.net ServerSocket]))

(defn write [writer s]
  (.write writer s)
  (.flush writer))

;; TODO gracefully handle closed sockets and unsubscribe from the bus
(defn read-loop [bus user reader]
  (async/go-loop [] ; TODO should this be thread since we're doing blocking io?
    (bus/post user {:type :prompt})
    (let [req (request/parse (.readLine reader))]
      (println "parsed request:" req)
      (case (:type req)
        :pub (do
               (bus/post user {:type :ok})
               (bus/pub bus (:chan req) (:msg req)))
        :sub (do
               (bus/post user {:type :ok})
               (bus/sub bus (:chan req) user))
        :unsub (do
                 (bus/post user {:type :ok})
                 (bus/unsub bus (:chan req) user))
        :err (bus/post user {:type :err
                             :err (:err req)})))
    (recur)))

;; Serialize writes through the user channel
(defn write-loop [user writer]
  (async/go-loop []
    (let [m (<! (:inbox user))]
      (case (:type m)
        ;; Note the carriage returns below to "move" the prompt when we get a
        ;; new message.
        :prompt (write writer "\r> ")
        :ok (write writer "\rOK\n> ")
        :msg (write writer (format "\rMSG %s %s\n> " (:chan m) (:msg m)))
        :err (write writer (format "\rERR %s\n> " (:err m)))))
    (recur)))

;; Blocks forever
(defn serve [bus port]
  (with-open [server-sock (ServerSocket. port)]
    (println (str "listening on :" port))
    (while true
      (let [sock (.accept server-sock)
            user (user/user)]
        (read-loop bus user (io/reader sock))
        (write-loop user (io/writer sock))))))
