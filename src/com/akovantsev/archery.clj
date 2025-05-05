(ns com.akovantsev.archery
  (:require
   [clojure.set :as set]))


(def _ (symbol "_"))
(declare _> some_>)

(defn -impl_> [x exprs]
  (let [;; for (refer-clojure exclude ->) + (refer _> rename {_> ->}):
        renames  (-> *ns*
                   ns-refers
                   set/map-invert
                   (select-keys [#'com.akovantsev.archery/_>
                                 #'com.akovantsev.archery/some_>])
                   vals
                   set)
        >?       (into #{(symbol "_>")
                         (symbol "some_>")
                         'com.akovantsev.archery/_>
                         'com.akovantsev.archery/some_>} renames)
        >form?   (fn [x] (and (-> x seq?) (-> x first >?)))
        has_?    (fn [x] (->> x (tree-seq coll? seq) (some #{_}) boolean))
        mb-wrap  (fn [x] (cond
                           (>form? x) x
                           (has_? x)  (list `as-> _ x)
                           :else      x))]
    (->> exprs (mapv mb-wrap) (cons x))))

(defmacro     _> [x & exprs]     `(-> ~@(-impl_> x exprs)))
(defmacro some_> [x & exprs] `(some-> ~@(-impl_> x exprs)))


(defn -mg [k]
  (fn
    ([x] (get x k))
    ([] (fn [x] (get x k)))))

(defn data-reader-for-getter [k]
  `((-mg ~k)))
