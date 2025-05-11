(ns com.akovantsev.archery-test-ns-renames
  (:require
   [com.akovantsev.archery-test :refer [assert-expands-to x]]
   [com.akovantsev.archery :refer [_> _>> some_> some_>> _]
    :rename {_>      lel
             _>>     kek
             some_>  somelel
             some_>> somekek}]))

;; aliases:
(assert-expands-to (lel x [_ _]) (let* [_ x] [_ _]))
(assert (nil? (somelel {} :a [_] first name)))
(assert-expands-to (kek x [_ _]) (let* [_ x] [_ _]))
(assert (nil? (somekek {} :a [_] first name)))
