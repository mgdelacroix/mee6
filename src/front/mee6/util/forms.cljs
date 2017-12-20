(ns mee6.util.forms
  (:require [cljs.spec.alpha :as s :include-macros true]
            [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str]))

;; --- Form Validation Api

(defn- interpret-problem
  [acc {:keys [path pred val via in] :as problem}]
  (cond
    (and (empty? path)
         (= (first pred) 'contains?))
    (let [path (conj path (last pred))]
      (update-in acc path assoc :missing))

    (and (seq path)
         (= 1 (count path)))
    (update-in acc path assoc :invalid)

    :else acc))

(defn validate
  [spec data]
  (when-not (s/valid? spec data)
    (let [report (s/explain-data spec data)]
      (reduce interpret-problem {} (::s/problems report)))))

(defn valid?
  [spec data]
  (s/valid? spec data))

;; --- Form Specs and Conformers

(def ^:private email-re
  #"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$")

(def ^:private number-re
  #"^[-+]?[0-9]*\.?[0-9]+$")

(def ^:private color-re
  #"^#[0-9A-Fa-f]{6}$")

(s/def ::email
  (s/and string? #(boolean (re-matches email-re %))))

(s/def ::non-empty-string
  (s/and string? #(not (str/empty? %))))

(defn- parse-number
  [v]
  (cond
    (re-matches number-re v) (js/parseFloat v)
    (number? v) v
    :else ::s/invalid))

(s/def ::string-number
  (s/conformer parse-number str))

;; --- Form UI

(mx/defc input-error
  [errors field]
  (when-let [error (get errors field)]
    [:ul.form-errors
     [:li {:key error} error]]))

(defn error-class
  [errors field]
  (when (get errors field)
    "invalid"))
