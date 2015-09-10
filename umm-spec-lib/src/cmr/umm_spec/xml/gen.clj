(ns cmr.umm-spec.xml.gen
  "Contains functions for generating XML using a Hiccup-style syntax."
  (:require [clojure.data.xml :as x]
            [cmr.umm-spec.simple-xpath :refer [select]]))

(defprotocol GenerateXML
  (generate [x]))

(extend-protocol GenerateXML
  clojure.lang.PersistentVector
  (generate [[tag maybe-attrs & content]]
            (let [[attrs content] (if (map? maybe-attrs)
                                       [maybe-attrs content]
                                       [{} (cons maybe-attrs content)])
                  content         (seq (remove nil? (generate content)))]
              (when content
                (apply x/element tag attrs content))))

  clojure.lang.ISeq
  (generate [xs] (map generate xs))

  java.lang.Number
  (generate [n] (str n))

  clojure.data.xml.Element
  (generate [el] el)

  String
  (generate [s] s)

  org.joda.time.DateTime
  (generate [d] (str d))

  java.lang.Boolean
  (generate [b] (str b))

  nil
  (generate [_] nil))

(defn xml
  "Returns XML string from structure describing XML elements."
  [structure]
  (x/indent-str (generate structure)))

;;; Helpers

(defn char-string
  "Returns an ISO gco:CharacterString element with the given contents."
  [content]
  [:gco:CharacterString content])

(defn char-string-at
  "Returns an ISO gco:CharacterString with contents taken from the given xpath."
  [context xpath]
  (char-string (select context xpath)))

(defn element-from
  [context kw]
  [kw {} (select context (name kw))])

(defn elements-from
  [context & kws]
  (map (partial element-from context) kws))