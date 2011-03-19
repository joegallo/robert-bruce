(ns robert.test.bruce
  (:use [robert.bruce])
  (:use [clojure.test])
  (:refer-clojure :exclude [double])
  (:import (java.io IOException)))

(deftest test-double
  (is (= 2 (double 1))))

(deftest test-exponential
  (is (= Math/E (exponential 1))))

(deftest test-golden-ratio
  (is (= 1.6180339887 (golden-ratio 1))))

(deftest test-catch
  (testing "catch allows a single exception or a collection"
    (is (= [Exception] (catch {:catch Exception})))
    (is (= [Exception] (catch {:catch [Exception]})))
    (is (= [Exception IOException] (catch {:catch [Exception IOException]})))))

(deftest test-decay
  (testing "decay allows nothing, a number, a function, or keywords"
    (is (= 1 ((decay {}) 1)))
    (is (= 1 ((decay {:decay 1}) 1)))
    (is (= 2 ((decay {:decay 2}) 1)))
    (is (= 2 ((decay {:decay double}) 1)))
    (is (= 2 ((decay {:decay :double}) 1)))
    (is (= Math/E ((decay {:decay :exponential}) 1)))
    (is (= 1.6180339887 ((decay {:decay :golden-ratio}) 1)))))

(deftest test-parse
  (testing "parse handles a variety of arguments correctly"
    (is (= [default-options identity []]
             (parse [identity])))
    (is (= [default-options identity ["a" "b"]]
             (parse [identity "a" "b"])))
    (is (= [(assoc default-options :tries 100) identity []]
             (parse [{:tries 100} identity])))
    (is (= [(assoc default-options :tries 100) identity ["a" "b"]]
             (parse [{:tries 100} identity "a" "b"]))))
  (testing "parse merges your options with the default options"
    (let [options {:a 1 :b 2 :sleep nil :tries 10}]
      (is (= [(merge default-options options) identity []]
               (parse [options identity])))))
  (testing "and with the metadata on the function you pass in"
    (let [options {:a 1 :b 2 :sleep nil :tries 10}]
      (is (= (merge default-options {:decay :foo} options)
             (first (parse [options ^{:decay :foo} #()]))))))
  (testing "priority is options, then meta, then defaults"
    (is (= 5 (:tries (first (parse [#()])))))
    (is (= 2 (:tries (first (parse [^{:tries 2} #()])))))
    (is (= 1 (:tries (first (parse [{:tries 1} #()])))))
    (is (= 1 (:tries (first (parse [{:tries 1} ^{:tries 2} #()])))))))

(deftest test-try-again?
  (testing "first, the exception must be acceptable"
    (is (try-again? {:catch [Exception] :tries 10} (IOException.)))
    (is (not (try-again? {:catch [IOException] :tries 10} (Exception.)))))
  (testing "second, there has to be enough tries left"
    (is (try-again? {:catch [Exception] :tries :unlimited} (IOException.)))
    (is (try-again? {:catch [Exception] :tries 10} (IOException.)))
    (is (try-again? {:catch [Exception] :tries 1} (IOException.)))
    (is (not (try-again? {:catch [Exception] :tries 0} (IOException.))))
    (is (not (try-again? {:catch [Exception] :tries -1} (IOException.))))))

(deftest test-update-tries
  (testing "tries should decrease by 1, unless it is :unlimited"
    (is (= {:tries 0} (update-tries {:tries 1})))
    (is (= {:tries 1} (update-tries {:tries 2})))
    (is (= {:tries :unlimited} (update-tries {:tries :unlimited})))))

(deftest test-update-sleep
  (testing "sleep should update with the decay function..."
    (is (= 2 (:sleep (update-sleep {:sleep 1 :decay 2}))))
    (is (= 2 (:sleep (update-sleep {:sleep 2 :decay identity})))))
  (testing "unless it is false or nil"
    (is (= false (:sleep (update-sleep {:sleep false :decay 2}))))
    (is (= nil (:sleep (update-sleep {:sleep nil :decay identity}))))))

(deftest test-retry
  (testing "success returns a result"
    (is (= 2 (retry default-options #(+ 1 1)))))
  (testing "failure returns a fn"
    (is (fn? (retry (assoc default-options :sleep nil) #(/ 1 0)))))
  (testing "unless you have run out of tries"
    (is (thrown? ArithmeticException
                 (retry (assoc default-options
                          :sleep nil
                          :tries 1)
                        #(/ 1 0))))))

(deftest test-try-try-again
  (testing "ten tries to do the job"
    (let [times (atom 0)]
      (is (thrown? ArithmeticException
                   (try-try-again {:sleep nil
                                   :tries 10}
                                  #(do (swap! times inc)
                                       (/ 1 0)))))
      (is (= 10 @times)))))
