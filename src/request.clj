(ns request
  (:require [clojure.string :as str]))

(defn split-first-word [s]
  (str/split s #" " 2))

(defn parse-pub [args]
  (let [[chan msg] (split-first-word args)]
    (if-not msg
      {:type :err
       :err "empty message"}
      {:type :pub
       :chan chan
       :msg msg})))

(defn parse-channel-op [type chan]
  (if (str/includes? chan " ")
    {:type :err
     :err "channel contains spaces"}
    {:type type
     :chan chan}))

;; The caller should check the :type key of the returned map for errors. I'm
;; not sure this is is idiomatic but it'll do for now.
(defn parse [line]
  (let [[op rest] (split-first-word line)]
    (case op
      "PUB" (parse-pub rest)
      "SUB" (parse-channel-op :sub rest)
      "UNSUB" (parse-channel-op :unsub rest)
      {:type :err
       :err (str "unknown op: " op)})))
