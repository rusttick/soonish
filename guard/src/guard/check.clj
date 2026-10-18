(ns guard.check
  "Pure checks over source files that have already been read.

  A file is a map:
    :file/path   the path relative to the project root
    :file/area   :src or :test
    :file/text   the whole text
    :file/forms  the top-level forms, read but not evaluated
    :file/error  why the file could not be read, in place of :file/forms

  Each check returns violations: maps with :violation/path,
  :violation/line, :violation/rule and :violation/message."
  (:require [clojure.string :as str]))

(def reading-ns-name
  "The namespace that auto-resolved keywords (::k) are read in, so that a
  keyword in it shows that the source used ::."
  "guard.reading")

(defn- walk-all
  "Every value inside form, form included."
  [form]
  (tree-seq coll? seq form))

(defn- core-name
  "The plain name of a symbol that is unqualified or from clojure.core."
  [sym]
  (when (contains? #{nil "clojure.core"} (namespace sym))
    (symbol (name sym))))

(defn- class-name?
  "True when s, such as \"java.time.Instant\", ends in a capitalised part."
  [s]
  (boolean (some-> s (str/split #"\.") last first Character/isUpperCase)))

(defn interop?
  "True for a symbol that calls Java: . .. new (.method x) (Class. args)
  and Class/member."
  [sym]
  (let [n (name sym)]
    (if (namespace sym)
      (class-name? (namespace sym))
      (or (contains? #{"." ".." "new"} n)
          (and (< 1 (count n))
               (or (str/starts-with? n ".") (str/ends-with? n ".")))))))

(defn- form-name
  "The name of a top-level definition such as (defn f ...)."
  [form]
  (when (and (seq? form) (symbol? (second form)))
    (second form)))

(defn deviation
  "The reason declared by ^{:deviation \"...\"} on a definition, if any."
  [form]
  (:deviation (meta (form-name form))))

(defn- violation
  [file form rule message]
  {:violation/path (:file/path file)
   :violation/line (:line (meta form))
   :violation/rule rule
   :violation/message message})

(defn- ns-form
  [forms]
  (first (filter #(and (seq? %) (= 'ns (first %))) forms)))

(defn- file-ns
  [file]
  (second (ns-form (:file/forms file))))

(defn core?
  "True for a source file whose namespace is in the pure core."
  [rules file]
  (boolean (and (= :src (:file/area file))
                (some->> (file-ns file)
                         str
                         (re-matches (re-pattern (:core-ns-pattern rules)))))))

(defn- banned-uses
  "A violation for each banned symbol in form, unless form is a
  definition that declares a deviation."
  [file banned rule form]
  (when-not (deviation form)
    (for [sym (distinct (filter symbol? (walk-all form)))
          :let [reason (get banned (core-name sym))]
          :when reason]
      (violation file form rule (str "`" sym "`: " reason)))))

(defn- interop-uses
  [file form]
  (when-not (deviation form)
    (for [sym (distinct (filter symbol? (walk-all form)))
          :when (interop? sym)]
      (violation file form :core/interop
                 (str "`" sym "` calls Java. Core uses Clojure functions,"
                      " and Java belongs in shell namespaces.")))))

(defn- ns-clauses
  [form]
  (filter seq? (drop 2 form)))

(defn required-libs
  "The namespaces a ns form requires."
  [form]
  (set (for [clause (ns-clauses form)
             :when (= :require (first clause))
             spec (rest clause)
             :let [lib (if (sequential? spec) (first spec) spec)]
             :when (symbol? lib)]
         lib)))

(defn- require-violations
  [rules file]
  (let [form (ns-form (:file/forms file))
        core-pattern (re-pattern (:core-ns-pattern rules))
        allowed? #(or (contains? (:core-may-require rules) %)
                      (re-matches core-pattern (str %)))]
    (concat
     (for [lib (required-libs form)
           :when (not (allowed? lib))]
       (violation file form :core/require
                  (str "core may not require `" lib "`. It requires only"
                       " other core namespaces and "
                       (str/join ", " (sort (:core-may-require rules))) ".")))
     (for [clause (ns-clauses form)
           :when (= :import (first clause))]
       (violation file form :core/import
                  "core may not import Java classes.")))))

(defn- deviation-violations
  [file]
  (for [form (:file/forms file)
        :let [reason (deviation form)]
        :when (some? reason)
        :when (not (and (string? reason) (not (str/blank? reason))))]
    (violation file form :deviation/reason
               "a deviation states its reason: ^{:deviation \"why\"}")))

(defn- destructuring-key?
  "True for keywords such as :task/keys, which destructure rather than
  name an attribute."
  [kw]
  (contains? #{"keys" "syms" "strs"} (name kw)))

(defn- keyword-violations
  [rules registry file]
  (for [form (:file/forms file)
        kw (distinct (filter qualified-keyword? (walk-all form)))
        :when (not (destructuring-key? kw))
        :when (not (contains? (:unregistered-keyword-namespaces rules)
                              (namespace kw)))
        :when (not (contains? registry kw))]
    (if (= reading-ns-name (namespace kw))
      (violation file form :keyword/auto-resolved
                 (str "`::" (name kw) "`: write the attribute's full name,"
                      " such as :task/estimate-days."))
      (violation file form :keyword/unregistered
                 (str "`" kw "` is not in the attribute registry. Check the"
                      " spelling, or ask the owner to add it to "
                      (:registry-path rules) ".")))))

(defn file-violations
  "Every rule that applies to one file."
  [rules registry file]
  (if-let [error (:file/error file)]
    [{:violation/path (:file/path file)
      :violation/rule :file/unreadable
      :violation/message (str "could not be read: " error
                              ". Check that brackets balance, and that it"
                              " does not use ::alias/k.")}]
    (let [forms (:file/forms file)]
      (concat
       (mapcat #(banned-uses file (:banned-everywhere rules)
                             :banned/everywhere %)
               forms)
       (when (core? rules file)
         (concat
          (mapcat #(banned-uses file (:banned-in-core rules) :banned/core %)
                  forms)
          (mapcat #(interop-uses file %) forms)
          (require-violations rules file)))
       (deviation-violations file)
       (keyword-violations rules registry file)))))

(defn registry-violations
  "Each registry entry needs a qualified name, a :doc string and a
  :schema."
  [rules registry]
  (for [[attr entry] (sort-by key registry)
        :when (not (and (qualified-keyword? attr)
                        (string? (:doc entry))
                        (contains? entry :schema)))]
    {:violation/path (:registry-path rules)
     :violation/rule :registry/entry
     :violation/message (str "`" attr "` needs a qualified name, a :doc"
                             " string and a :schema.")}))

(defn- qualified
  [ns-sym var-name]
  (symbol (str ns-sym) (str var-name)))

(defn unused-public-vars
  "Public vars defined in src that nothing in src or test uses, other
  than the entry points in the rules. A var's use of itself does not
  count. analysis is clj-kondo's analysis of src and test."
  [rules {:keys [var-definitions var-usages]}]
  (let [used (set (for [usage var-usages
                        :when (not (and (= (:from usage) (:to usage))
                                        (= (:from-var usage) (:name usage))))]
                    (qualified (:to usage) (:name usage))))]
    (for [definition (sort-by (juxt :filename :row) var-definitions)
          :let [var-sym (qualified (:ns definition) (:name definition))]
          :when (str/starts-with? (:filename definition) "src/")
          :when (not (:private definition))
          :when (not (contains? used var-sym))
          :when (not (contains? (:entry-points rules) var-sym))]
      {:violation/path (:filename definition)
       :violation/line (:row definition)
       :violation/rule :var/unused
       :violation/message (str "`" var-sym "` is public, but nothing in src"
                               " or test uses it. Delete it if it is dead."
                               " If something outside uses it, ask the owner"
                               " to add it to :entry-points in"
                               " guard/rules.edn.")})))

(defn- count-symbols
  [names forms]
  (count (for [form forms
               sym (walk-all form)
               :when (and (symbol? sym)
                          (contains? names (core-name sym)))]
           sym)))

(defn counts
  "What the ratchet measures, over files that could be read."
  [rules files]
  (let [src-forms (mapcat :file/forms (filter #(= :src (:file/area %)) files))]
    {:deviations (count (keep deviation (mapcat :file/forms files)))
     :references (count-symbols (get-in rules [:ratchet :references]) src-forms)
     :loops (count-symbols (get-in rules [:ratchet :loops]) src-forms)
     :dynamic-vars (count (filter #(:dynamic (meta (form-name %))) src-forms))
     :lint-suppressions (count (mapcat #(re-seq #"clj-kondo/ignore" (:file/text %))
                                       files))}))

(defn ratchet-violations
  "A count above its baseline fails. Only the owner raises a baseline."
  [baseline measured]
  (for [[measure n] (sort-by key measured)
        :let [allowed (get baseline measure 0)]
        :when (> n allowed)]
    {:violation/path "guard/baseline.edn"
     :violation/rule :ratchet/increase
     :violation/message (str (name measure) " rose from " allowed " to " n
                             ". Remove the new ones, or explain to the owner"
                             " why they are needed.")}))

(defn ratchet-decreases
  "Counts that fell below their baseline, which the owner may lower."
  [baseline measured]
  (for [[measure n] (sort-by key measured)
        :let [allowed (get baseline measure 0)]
        :when (< n allowed)]
    {:ratchet/measure measure :ratchet/baseline allowed :ratchet/now n}))

(defn sizes
  "Lines of source in core and in shell. The guide asks for a large core
  and a thin shell."
  [rules files]
  (let [lines #(count (str/split-lines (:file/text %)))
        src (filter #(= :src (:file/area %)) files)
        {core true shell false} (group-by #(core? rules %) src)]
    {:core-lines (apply + (map lines core))
     :shell-lines (apply + (map lines shell))}))
