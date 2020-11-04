(ns request-test
  (:require
    [clojure.test :refer [deftest are is]]
    [request]))

(deftest test-split-first-word
  (is (= (request/split-first-word "foo bar baz") ["foo" "bar baz"])))

(deftest test-parse
  (are [line expected] (= (request/parse line) expected)
    "PUB #foo hello world" {:type :pub :chan "#foo" :msg "hello world"}
    "SUB #foo"             {:type :sub :chan "#foo"}
    "UNSUB #foo"           {:type :unsub :chan "#foo"}
    "NICK @alice"          {:type :nick :nick "@alice"}
    "FOAF"                 {:type :foaf}
    "PUB #foo"             {:type :err :err "empty message"}
    "SUB #foo bar"         {:type :err :err "channel contains spaces"}
    "UNSUB #foo bar"       {:type :err :err "channel contains spaces"}
    "NICK Ms Alice"        {:type :err :err "nick contains spaces"}
    "PUG #foo bar"         {:type :err :err "unknown op: PUG"}))
