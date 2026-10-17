(ns soonish.core.schedule
  "Step 0 of the design: the forward pass of the critical path method,
  with estimates taken as durations.

  Tasks are a map from task id to a map of task attributes. Ids must be
  comparable with each other, such as all keywords or all strings. Each
  function reads only the attributes it names.

  The backward pass and the critical path come next."
  (:require [clojure.set :as set]))

(defn unknown-tasks
  "Ids that some task waits for but that are not tasks."
  [tasks]
  (set (for [[_ task] tasks
             id (:task/after task)
             :when (not (contains? tasks id))]
         id)))

(defn successors
  "Task id to the ids of the tasks that wait for it."
  [tasks]
  (apply merge-with set/union
         (zipmap (keys tasks) (repeat #{}))
         (for [[id task] tasks
               before (:task/after task)]
           {before #{id}})))

(defn- kahn-step
  "Moves the first ready task to the order, and makes ready the tasks
  that were waiting only for it."
  [successors-of {:keys [ready waiting order]}]
  (let [id (first ready)
        waiting' (merge-with - waiting (zipmap (successors-of id) (repeat 1)))
        released (filter #(zero? (waiting' %)) (successors-of id))]
    {:ready (into (disj ready id) released)
     :waiting waiting'
     :order (conj order id)}))

(defn topological-order
  "Task ids ordered so that every task comes after the tasks it waits
  for, by Kahn's algorithm (Kahn 1962). Ties are broken by id, so the
  same tasks always give the same order. Every id in :task/after must be
  a task.

  Returns {:schedule/order [id ...]}, or {:schedule/cycle #{id ...}}
  when no order exists."
  [tasks]
  (let [successors-of (successors tasks)
        waiting (update-vals tasks (comp count set :task/after))
        start {:ready (into (sorted-set) (filter #(zero? (waiting %))) (keys tasks))
               :waiting waiting
               :order []}
        {:keys [order]} (->> start
                             (iterate #(kahn-step successors-of %))
                             (drop-while (comp seq :ready))
                             first)]
    (if (= (count order) (count tasks))
      {:schedule/order order}
      {:schedule/cycle (set/difference (set (keys tasks)) (set order))})))

(defn- start-day
  "The recorded actual start, or else the day the last task waited for
  finishes."
  [times task]
  (or (:task/actual-start-day task)
      (apply max 0 (map #(get-in times [% :schedule/finish-day])
                        (:task/after task)))))

(defn forward-pass
  "Start and finish day of every task, taking the tasks in order, which
  comes from topological-order. Days are working days from day 0.

  A recorded actual start or finish replaces the computed one, and the
  tasks after it move. A task needs :task/estimate-days unless it has
  :task/actual-finish-day."
  [tasks order]
  (reduce (fn [times id]
            (let [task (tasks id)
                  start (start-day times task)]
              (assoc times id
                     {:schedule/start-day start
                      :schedule/finish-day (or (:task/actual-finish-day task)
                                               (+ start (:task/estimate-days task)))})))
          {}
          order))

(defn finish-day
  "The day the last task finishes."
  [times]
  (apply max 0 (map :schedule/finish-day (vals times))))

(defn schedule
  "The Step 0 schedule of tasks, or a description of why there is none.

  Returns {:schedule/times {id {:schedule/start-day d
                                :schedule/finish-day d}}},
  or {:schedule/unknown-tasks #{id ...}},
  or {:schedule/cycle #{id ...}}."
  [tasks]
  (let [unknown (unknown-tasks tasks)]
    (if (seq unknown)
      {:schedule/unknown-tasks unknown}
      (let [result (topological-order tasks)]
        (if-let [order (:schedule/order result)]
          {:schedule/times (forward-pass tasks order)}
          result)))))
