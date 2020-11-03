;;;; Adapted from
;;;; https://github.com/clojure-cookbook/clojure-cookbook/blob/master/05_network-io/5-10_tcp-server.asciidoc

(ns server
  (:require
    [clojure.java.io :as io]
    [clojure.core.async :as async]
    [request])
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

(defn handle-pub [sock]
  prn "handle-pub")

(defn handle-sub [sock]
  prn "handle-sub")

(defn handle [sock]
  ;; TODO gracefully handle closed sockets
  (let [req (request/parse (receive sock))]
    (if (:err req)
      (write sock (str "ERR " (:err req)))
      (do
        (case (:op req)
          :pub (handle-pub sock)
          :sub (handle-sub sock))
        (write sock "OK")
        (recur sock)))))

;; Returns an atom, stop the server by setting it to false -- at which point we
;; accept one more socket then exit.
;; TODO close server without having to accept one more socket. Can we do this
;; with async/chan
(defn serve [port]
  (let [running (atom true)]
    (async/go
      (with-open [server-sock (ServerSocket. port)]
        (while @running
          (let [sock (.accept server-sock)]
            (async/go (handle sock))))))
    running))
