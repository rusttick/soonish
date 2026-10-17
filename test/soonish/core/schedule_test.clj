(ns soonish.core.schedule-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [soonish.core.schedule :as schedule]))

(def running-example
  "The five tasks of the running example in doc/design3.md."
  {:A {:task/estimate-days 3}
   :B {:task/estimate-days 5 :task/after #{:A}}
   :C {:task/estimate-days 4 :task/after #{:A}}
   :D {:task/estimate-days 2 :task/after #{:B :C}}
   :E {:task/estimate-days 3 :task/after #{:D}}})

(defn- days
  "Task id to [start finish], which is easier to read in a test."
  [times]
  (update-vals times (juxt :schedule/start-day :schedule/finish-day)))

(deftest running-example-dates
  (let [times (:schedule/times (schedule/schedule running-example))]
    (is (= {:A [0 3] :B [3 8] :C [3 7] :D [8 10] :E [10 13]}
           (days times)))
    (is (= 13 (schedule/finish-day times)))))

(deftest actual-dates-replace-computed-ones
  (testing "B finished on day 10 instead of 8, so D and E move"
    (let [tasks (assoc-in running-example [:B :task/actual-finish-day] 10)
          times (:schedule/times (schedule/schedule tasks))]
      (is (= [10 12] (:D (days times))))
      (is (= 15 (schedule/finish-day times)))))
  (testing "C started on day 5 instead of 3, so it now ends after B"
    (let [tasks (assoc-in running-example [:C :task/actual-start-day] 5)
          times (:schedule/times (schedule/schedule tasks))]
      (is (= [5 9] (:C (days times))))
      (is (= 14 (schedule/finish-day times))))))

(deftest problems-are-described-as-data
  (is (= {:schedule/unknown-tasks #{:Z}}
         (schedule/schedule (assoc-in running-example [:A :task/after] #{:Z}))))
  (is (= {:schedule/cycle #{:A :B :C :D :E}}
         (schedule/schedule (assoc-in running-example [:A :task/after] #{:E})))))

(defn- gen-after
  "Up to three ids for task i to wait for, all smaller than i."
  [i]
  (if (zero? i)
    (gen/return #{})
    (gen/fmap set (gen/vector (gen/choose 0 (dec i)) 0 3))))

(defn- gen-task
  "Task i, with an estimate and the tasks it waits for."
  [i]
  (gen/hash-map :task/estimate-days (gen/choose 1 20)
                :task/after (gen-after i)))

(def gen-tasks
  "Tasks 0 to n-1, where each task waits only for tasks with smaller ids,
  so there is never a cycle."
  (gen/bind (gen/choose 1 30)
            (fn [n]
              (gen/fmap #(zipmap (range n) %)
                        (apply gen/tuple (map gen-task (range n)))))))

(defspec the-order-puts-each-task-after-what-it-waits-for 200
  (prop/for-all [tasks gen-tasks]
    (let [order (:schedule/order (schedule/topological-order tasks))
          position (zipmap order (range))]
      (and (= (count tasks) (count order) (count position))
           (every? (fn [[id task]]
                     (every? #(< (position %) (position id)) (:task/after task)))
                   tasks)))))

(defspec each-task-starts-after-what-it-waits-for 200
  (prop/for-all [tasks gen-tasks]
    (let [times (:schedule/times (schedule/schedule tasks))]
      (every? (fn [[id task]]
                (let [{:schedule/keys [start-day finish-day]} (times id)]
                  (and (= finish-day (+ start-day (:task/estimate-days task)))
                       (every? #(<= (get-in times [% :schedule/finish-day]) start-day)
                               (:task/after task)))))
              tasks))))
