(ns soonish.core.attrs
  "Every attribute this program uses, defined once.

  An attribute is a qualified keyword whose meaning holds in any map it
  appears in. bin/check fails on a qualified keyword that is not listed
  here, so a misspelt key cannot pass silently.

  Entries are only ever added. Never remove one or change its meaning;
  if the meaning changes, add a new name. Only the owner changes this
  file.

  :schema is written in Malli syntax. It is data, and nothing enforces
  it yet.")

(def registry
  "Attribute to {:doc string, :schema Malli form}."
  {;; Tasks, as the planner enters them

   :task/estimate-days
   {:doc "Working days that the person doing the task thinks are equally
  likely to be too many or too few: a median."
    :schema [:and number? [:> 0]]}

   :task/after
   {:doc "Ids of the tasks that must finish before this one can start."
    :schema [:set any?]}

   :task/actual-start-day
   {:doc "The working day on which the task actually started, counted
  from day 0 of the project."
    :schema [:and number? [:>= 0]]}

   :task/actual-finish-day
   {:doc "The working day on which the task actually finished, counted
  from day 0 of the project."
    :schema [:and number? [:>= 0]]}

   ;; Schedules, as computed from tasks

   :schedule/start-day
   {:doc "The computed working day on which a task starts."
    :schema number?}

   :schedule/finish-day
   {:doc "The computed working day on which a task finishes."
    :schema number?}

   :schedule/times
   {:doc "Task id to that task's :schedule/start-day and
  :schedule/finish-day."
    :schema [:map-of any? map?]}

   :schedule/order
   {:doc "Task ids in an order where every task comes after the tasks it
  waits for."
    :schema [:vector any?]}

   :schedule/cycle
   {:doc "Ids of the tasks on a dependency cycle, or waiting behind one,
  so that no order exists."
    :schema [:set any?]}

   :schedule/unknown-tasks
   {:doc "Ids named in some :task/after that are not tasks."
    :schema [:set any?]}})
