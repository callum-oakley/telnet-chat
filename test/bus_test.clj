(ns bus-test
  (:require
    [clojure.core.async :as async :refer [timeout go alts! <!! <!]]
    [clojure.test :refer [deftest is]]
    [bus]
    [user]))

(defn <!!!
  "Either receives from ch in under 100ms, or returns :timeout"
  [ch]
  (first (<!! (go (alts! [ch (go (<! (timeout 100)) :timeout)])))))

(deftest test-bus-publish-fanout
  (let [bus (bus/bus)
        alice (user/user)
        bob (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/sub bus "#foo" bob)
    (bus/pub bus "#foo" "@admin" "hello world")
    (is (=
         (<!!! (:inbox alice))
         {:type :msg :chan "#foo" :from "@admin" :msg "hello world"}))
    (is (=
         (<!!! (:inbox bob))
         {:type :msg :chan "#foo" :from "@admin" :msg "hello world"}))))

(deftest test-bus-unsubscribe
  (let [bus (bus/bus)
        alice (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/pub bus "#foo" "@admin" "hello world")
    (is (=
         (<!!! (:inbox alice))
         {:type :msg :chan "#foo" :from "@admin" :msg "hello world"}))
    (bus/unsub bus "#foo" alice)
    (bus/pub bus "#foo" "@admin" "still there?")
    (is (= (<!!! (:inbox alice)) :timeout))))

(deftest test-bus-routing
  (let [bus (bus/bus)
        alice (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/pub bus "#foo" "@admin" "hello #foo")
    (is (=
         (<!!! (:inbox alice))
         {:type :msg :chan "#foo" :from "@admin" :msg "hello #foo"}))
    (bus/pub bus "#bar" "@admin" "hello #bar")
    (is (= (<!!! (:inbox alice)) :timeout))))

(deftest test-update-all
  (is (=
       (bus/update-all {:a 0 :b 1 :c 2} inc)
       {:a 1 :b 2 :c 3}))
  (is (=
       (bus/update-all {:a #{:x :y} :b #{:x} :c #{:y}} disj :x)
        {:a #{:y} :b #{} :c #{:y}})))

(deftest test-unsub-all
  (let [bus (bus/bus)
        alice (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/sub bus "#bar" alice)
    (bus/unsub-all bus alice)
    (bus/pub bus "#foo" "@admin" "hello #foo")
    (bus/pub bus "#bar" "@admin" "hello #bar")
    (is (= (<!!! (:inbox alice)) :timeout))))
