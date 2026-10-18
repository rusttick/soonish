(ns guard.main
  "Runs the project's own rules over src and test:

    clojure -M:guard

  The owner, and only the owner, accepts new counts with:

    clojure -M:guard --update-baseline"
  (:require [clj-kondo.core :as kondo]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [guard.check :as check]))

(def ^:private rules-path "guard/rules.edn")
(def ^:private baseline-path "guard/baseline.edn")

(defn- read-forms
  "Every top-level form in text, read without evaluating anything."
  [^String text]
  (let [eof (Object.)
        reader (clojure.lang.LineNumberingPushbackReader.
                (java.io.StringReader. text))]
    (binding [*ns* (create-ns (symbol check/reading-ns-name))
              *read-eval* false]
      (into []
            (take-while #(not (identical? eof %)))
            (repeatedly #(read {:eof eof :read-cond :allow} reader))))))

(defn- read-file
  [area ^java.io.File file]
  (let [text (slurp file)
        base {:file/path (.getPath file) :file/area area :file/text text}]
    (try
      (assoc base :file/forms (read-forms text))
      (catch Exception e
        (assoc base :file/error (ex-message e))))))

(defn- source-files
  [dir area]
  (->> (file-seq (io/file dir))
       (filter #(re-find #"\.clj[cs]?$" (.getName ^java.io.File %)))
       (sort-by #(.getPath ^java.io.File %))
       (mapv #(read-file area %))))

(defn- analyse
  "clj-kondo's analysis of every definition and use in src and test."
  []
  (:analysis (kondo/run! {:lint ["src" "test"] :config {:analysis true}})))

(defn- describe
  [{:violation/keys [path line rule message]}]
  (str path (when line (str ":" line)) "  " (name rule) "  " message))

(defn- update-baseline!
  [counts]
  (spit baseline-path
        (str ";; Written by `clojure -M:guard --update-baseline`."
             " Only the owner runs it.\n"
             (with-out-str (pprint/pprint (into (sorted-map) counts)))))
  (println "Baseline written:" (into (sorted-map) counts)))

(defn- report!
  [rules registry baseline files counts]
  (let [readable (remove :file/error files)
        violations (concat (check/registry-violations rules registry)
                           (mapcat #(check/file-violations rules registry %)
                                   files)
                           (check/unused-public-vars rules (analyse))
                           (check/ratchet-violations baseline counts))]
    (run! (comp println describe) violations)
    (run! #(println (str "Ratchet: " (name (:ratchet/measure %)) " fell to "
                         (:ratchet/now %) " from " (:ratchet/baseline %)
                         ". The owner may lower the baseline."))
          (check/ratchet-decreases baseline counts))
    (println "Sizes:" (check/sizes rules readable))
    (if (seq violations)
      (do (println (count violations) "guard violations.")
          (System/exit 1))
      (println "Guard: no violations."))))

(defn -main
  "Checks src and test, and exits with status 1 on any violation."
  [& args]
  (let [rules (edn/read-string (slurp rules-path))
        registry @(requiring-resolve (:registry-var rules))
        files (into (source-files "src" :src) (source-files "test" :test))
        counts (check/counts rules (remove :file/error files))]
    (if (some #{"--update-baseline"} args)
      (update-baseline! counts)
      (report! rules registry (edn/read-string (slurp baseline-path))
               files counts))))
