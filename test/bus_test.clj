(ns bus-test
  (:require
    [clojure.core.async :as async :refer [timeout go alts! <!!]]
    [clojure.test :refer [deftest is]]
    [bus]
    [user]))

(deftest test-bus-publish-fanout
  (let [bus (bus/bus)
        alice (user/user)
        bob (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/sub bus "#foo" bob)
    (bus/pub bus "#foo" "hello world")
    (is (= (<!! (:inbox alice)) {:type :msg :chan "#foo" :msg "hello world"}))
    (is (= (<!! (:inbox bob)) {:type :msg :chan "#foo" :msg "hello world"}))))

(deftest test-bus-unsubscribe
  (let [bus (bus/bus)
        alice (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/pub bus "#foo" "hello world")
    (is (= (<!! (:inbox alice)) {:type :msg :chan "#foo" :msg "hello world"}))
    (bus/unsub bus "#foo" alice)
    (bus/pub bus "#foo" "still there?")
    ;; Check that we don't get a message within 100 ms, but we get nil from the
    ;; timeout instead
    (is (= (first (<!! (go (alts! [(:inbox alice) (timeout 100)])))) nil))))

(deftest test-bus-routing
  (let [bus (bus/bus)
        alice (user/user)]
    (bus/sub bus "#foo" alice)
    (bus/pub bus "#foo" "hello #foo")
    (is (= (<!! (:inbox alice)) {:type :msg :chan "#foo" :msg "hello #foo"}))
    (bus/pub bus "#bar" "hello #bar")
    ;; Check that we don't get a message within 100 ms, but we get nil from the
    ;; timeout instead
    (is (= (first (<!! (go (alts! [(:inbox alice) (timeout 100)])))) nil))))
