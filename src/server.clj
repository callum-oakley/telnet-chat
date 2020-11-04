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

(defn write-message [writer m]
  (case (:type m)
    ;; Note the carriage returns below to "move" the prompt when we get a
    ;; new message.
    :prompt (write writer "\r> ")
    :msg (write writer (format "\rMSG %s %s %s\n> "
                         (:chan m) (:from m) (:msg m)))
    :err (write writer (format "\rERR %s\n> " (:err m)))))

(defn process-request [bus user req]
  (println "processing request:" req)
  (case (:type req)
    :pub (bus/pub bus (:chan req) (user/nick user) (:msg req))
    :sub (bus/sub bus (:chan req) user)
    :unsub (bus/unsub bus (:chan req) user)
    :nick (user/identify user (:nick req))
    :err (bus/post user {:type :err
                         :err (:err req)})))

(defn read-loop [bus user reader]
  (async/go-loop [] ; TODO should this be thread since we're doing blocking io?
    (bus/post user {:type :prompt})
    (if-let [line (.readLine reader)]
      (do
        (process-request bus user (request/parse line))
        (recur))
      ;; Unsubscribe from all channels and signal for the write-loop to exit by
      ;; closing the user's inbox channel.
      (do
        (bus/unsub-all bus user)
        (async/close! (:inbox user))))))

;; Serialize writes through the user's inbox channel. Exits when the channel is
;; closed.
(defn write-loop [user writer sock]
  (async/go-loop []
    (if-let [m (<! (:inbox user))]
      (do
        (write-message writer m)
        (recur))
      (.close sock))))

;; Blocks forever
(defn serve [bus port]
  (with-open [server-sock (ServerSocket. port)]
    (println (str "listening on :" port))
    (while true
      (let [sock (.accept server-sock)
            user (user/user)]
        (read-loop bus user (io/reader sock))
        (write-loop user (io/writer sock) sock)))))
