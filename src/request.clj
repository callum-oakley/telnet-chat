(ns request
  (:require [clojure.string :as str]))

(defn split-first-word [s]
  (str/split s #" " 2))

(defn parse-pub [args]
  (let [[chan msg] (split-first-word args)]
    (if-not msg
      {:err "empty message"}
      {:op :pub
       :chan chan
       :msg msg})))

(defn parse-sub [args]
  (let [[chan rest] (split-first-word args)]
    (if rest
      {:err "channel contains spaces"}
      {:op :sub
       :chan chan})))

;; The caller should check the return value for an :err, I'm sure this isn't
;; idiomatic but it'll do for now.
(defn parse [line]
  (let [[op rest] (split-first-word line)]
    (case op
      "PUB" (parse-pub rest)
      "SUB" (parse-sub rest)
      {:err (str "unknown op: " op)})))
