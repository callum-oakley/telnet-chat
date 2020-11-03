;;;; Adapted from
;;;; https://github.com/clojure-cookbook/clojure-cookbook/blob/master/05_network-io/5-10_tcp-server.asciidoc

(ns server
  (:require
    [clojure.java.io :as io]
    [clojure.core.async :as async]
    [request]
    [bus])
  (:import [java.net ServerSocket]))

;;;; TODO handle non-text input
;;;; io/reader and io/writer assume everything is text. This fits our toy
;;;; protocol, but you probably get a pretty knarly error if you send non-text.

(defn receive
  "Read a line of textual data from the given socket"
  [socket]
  (.readLine (io/reader socket)))

(defn write
  "Send the given line out over the given socket, appends a newline"
  [socket line]
  (let [writer (io/writer socket)]
    (.write writer (str line \newline))
    (.flush writer)))

(defn handle-pub [bus sock {chan :chan msg :msg}]
  (bus/pub bus chan msg))

(defn handle-sub [bus sock {chan :chan}]
  (async/go
    (let [subscription (bus/sub bus chan)]
      (while true
        (->> subscription
          async/<!
          (str "MSG ")
          (write sock))))))

(defn handle [bus sock]
  ;; TODO gracefully handle closed sockets and unsubscribe from the bus
  (let [req (request/parse (receive sock))]
    (prn req)
    (if (:err req)
      (write sock (str "ERR " (:err req)))
      (do
        (write sock "OK")
        (case (:op req)
          :pub (handle-pub bus sock req)
          :sub (handle-sub bus sock req))))
    (recur bus sock)))

;; Blocks forever
(defn serve [bus port]
  (with-open [server-sock (ServerSocket. port)]
    (println (str "listening on :" port))
    (while true
      (let [sock (.accept server-sock)]
        (async/thread (handle bus sock))))))
