(ns foaf
  (:require
    [grafter-2.rdf.protocols :as pr :refer [add]]
    [grafter-2.rdf4j.io :as sut]
    [grafter-2.rdf4j.templater :refer [graph]]
    [grafter.vocabularies.core :refer [prefixer ->uri]]
    [grafter.vocabularies.foaf :refer [foaf foaf:Person foaf:nick foaf:name]]
    [grafter.vocabularies.rdf :refer [rdf:a]]))

(def foaf:member (foaf "member"))
(def foaf:Group (foaf "Group"))

(def base "https://callumoakley.net/tc")
(def tc:channel (prefixer (str base "/channel/")))
(def tc:user (prefixer (str base "/user/")))

(defn report-user [{nick :nick}]
  [(tc:user @nick) [rdf:a foaf:Person] [foaf:nick @nick]])

(defn report-channel [[chan users]]
  (into
    [(tc:channel chan) [rdf:a foaf:Group] [foaf:name chan]]
    (map (fn [{nick :nick}] [foaf:member (tc:user @nick)]) users)))

(defn unique-users [bus]
  (->> bus
    vals
    (apply concat)
    (into #{})))

(defn report
  "Produces an RDF model of the shape of the network -- formatted as ttl"
  [bus]
  (let [writer (java.io.StringWriter.)]
    (add
      (sut/rdf-writer writer :format :ttl)
      (apply graph (->uri base)
        (concat
          (map report-channel bus)
          (map report-user (unique-users bus)))))
    (str writer)))
