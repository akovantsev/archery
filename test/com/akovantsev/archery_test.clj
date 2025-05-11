(ns com.akovantsev.archery-test
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [macroexpand-all]]
   [com.akovantsev.archery :as ar :refer [_> _>> some_> some_>> _]]))


(defmacro assert-expands-to [in out]
  (let [expanded (macroexpand-all in)]
    `(when-not (= '~expanded '~out)
       (throw
         (new AssertionError
           (str/join "\n"
             ['~in "expands to:" '~expanded "expected:" '~out]))))))

(defmacro assert= [in out]
  `(let [res# ~in]
     (when-not (= res# '~out)
       (throw
         (new AssertionError
           (str/join "\n"
             ["not=" '~in "actual:" res# "expected:" '~out]))))))


(def x (symbol "x"))


(assert-expands-to (_> x +) (+ x))
(assert-expands-to (_> x (+)) (+ x))
(assert-expands-to (_> x (+ 1)) (+ x 1))
(assert-expands-to (_>> x +) (+ x))
(assert-expands-to (_>> x (+)) (+ x))
(assert-expands-to (_>> x (+ 1)) (+ 1 x))

(assert-expands-to (_> x (+ _ 1))     (let* [_ x] (+ _ 1)))
(assert-expands-to (_> x (+ 1 _))     (let* [_ x] (+ 1 _)))
(assert-expands-to (_> x (+ 1 _ 2))   (let* [_ x] (+ 1 _ 2)))
(assert-expands-to (_> x (+ 1 _ 2 _)) (let* [_ x] (+ 1 _ 2 _)))
(assert-expands-to (_>> x (+ _ 1))     (let* [_ x] (+ _ 1)))
(assert-expands-to (_>> x (+ 1 _))     (let* [_ x] (+ 1 _)))
(assert-expands-to (_>> x (+ 1 _ 2))   (let* [_ x] (+ 1 _ 2)))
(assert-expands-to (_>> x (+ 1 _ 2 _)) (let* [_ x] (+ 1 _ 2 _)))

(assert-expands-to (_> x [])    ([] x))
(assert-expands-to (_> x [_])   (let* [_ x] [_]))
(assert-expands-to (_> x [_ _]) (let* [_ x] [_ _]))
(assert-expands-to (_>> x [])    ([] x))
(assert-expands-to (_>> x [_])   (let* [_ x] [_]))
(assert-expands-to (_>> x [_ _]) (let* [_ x] [_ _]))

(assert-expands-to (_> x {})    ({} x))
(assert-expands-to (_> x {1 _}) (let* [_ x] {1 _}))
(assert-expands-to (_> x {_ 1}) (let* [_ x] {_ 1}))
(assert-expands-to (_> x {_ _}) (let* [_ x] {_ _}))
(assert-expands-to (_>> x {})    ({} x))
(assert-expands-to (_>> x {1 _}) (let* [_ x] {1 _}))
(assert-expands-to (_>> x {_ 1}) (let* [_ x] {_ 1}))
(assert-expands-to (_>> x {_ _}) (let* [_ x] {_ _}))

(assert-expands-to (_> x [{}])    ([{}] x))
(assert-expands-to (_> x [{1 _}]) (let* [_ x] [{1 _}]))
(assert-expands-to (_> x [{_ 1}]) (let* [_ x] [{_ 1}]))
(assert-expands-to (_> x [{_ _}]) (let* [_ x] [{_ _}]))
(assert-expands-to (_>> x [{}])    ([{}] x))
(assert-expands-to (_>> x [{1 _}]) (let* [_ x] [{1 _}]))
(assert-expands-to (_>> x [{_ 1}]) (let* [_ x] [{_ 1}]))
(assert-expands-to (_>> x [{_ _}]) (let* [_ x] [{_ _}]))

(assert-expands-to (_> x inc [_ _])          (let* [_ (inc x)] [_ _]))
(assert-expands-to (_> x inc [_ (_> [_])])   (let* [_ (inc x)] [_ [_]]))
(assert-expands-to (_> x inc [_ (_> _ [_])]) (let* [_ (inc x)] [_ (let* [_ _] [_])]))
(assert-expands-to (_> x inc [_ (_> _ [_])]) (let* [_ (inc x)] [_ (let* [_ _] [_])]))
(assert-expands-to (_>> x inc [_ _])          (let* [_ (inc x)] [_ _]))
(assert-expands-to (_>> x inc [_ (_> [_])])   (let* [_ (inc x)] [_ [_]]))
(assert-expands-to (_>> x inc [_ (_> _ [_])]) (let* [_ (inc x)] [_ (let* [_ _] [_])]))
(assert-expands-to (_>> x inc [_ (_> _ [_])]) (let* [_ (inc x)] [_ (let* [_ _] [_])]))

(assert-expands-to (_> x inc (if 1 2 _))         (let* [_ (inc x)] (if 1 2 _)))
(assert-expands-to (_> x inc (if (zero? _) 2 _)) (let* [_ (inc x)] (if (zero? _) 2 _)))
(assert-expands-to (_>> x inc (if 1 2 _))         (let* [_ (inc x)] (if 1 2 _)))
(assert-expands-to (_>> x inc (if (zero? _) 2 _)) (let* [_ (inc x)] (if (zero? _) 2 _)))


(assert-expands-to (_> x (/ _ 0) (try (catch Exception e :kek)))   (try (let* [_ x] (/ _ 0)) (catch Exception e :kek)))
(assert-expands-to (_> x (/ _ 0) (try _ (catch Exception e :kek))) (let* [_ (let* [_ x] (/ _ 0))] (try _ (catch Exception e :kek))))
(assert-expands-to (_> x (/ _ 0) (try (catch Exception e _)))      (let* [_ (let* [_ x] (/ _ 0))] (try (catch Exception e _))))
(assert-expands-to (_>> x (/ _ 0) (try (catch Exception e :kek)))   (try (catch Exception e :kek) (let* [_ x] (/ _ 0))))
(assert-expands-to (_>> x (/ _ 0) (try _ (catch Exception e :kek))) (let* [_ (let* [_ x] (/ _ 0))] (try _ (catch Exception e :kek))))
(assert-expands-to (_>> x (/ _ 0) (try (catch Exception e _)))      (let* [_ (let* [_ x] (/ _ 0))] (try (catch Exception e _))))

(assert-expands-to (_> x  (map inc) (filter nil?) (remove _ _) inc)  (inc (let* [_ (filter (map x inc) nil?)] (remove _ _))))
(assert-expands-to (_>> x (map inc) (filter nil?) (remove _ _) inc)  (inc (let* [_ (filter nil? (map inc x))] (remove _ _))))


(assert-expands-to
  (_>> x
    (into []
      (comp
        (map inc)
        (remove dec))))
  (into []
    (comp
      (map inc)
      (remove dec))
    x))

(assert-expands-to
  (_>> x
    (into []
      (comp
        (map inc _)
        (remove dec))))
  (let* [_ x]
    (into []
      (comp
        (map inc _)
        (remove dec)))))

(assert-expands-to
  (_>> x
    (into []
      (comp
        (map inc _)
        (remove dec))
      _))
  (let* [_ x]
    (into []
      (comp
        (map inc _)
        (remove dec))
      _)))



(assert-expands-to
  (_> x
    inc
    (do
      (println _)
      [_ _])
    {:a _})

  (let* [_ (let* [_ (inc x)]
             (do (println _)
                 [_ _]))]
    {:a _}))

(assert-expands-to
  (_>> x
    inc
    (do
      (println _)
      [_ _])
    {:a _})

  (let* [_ (let* [_ (inc x)]
             (do (println _)
                 [_ _]))]
    {:a _}))


(assert= (_> x [_ _] (for [y _ z [1 2]] [y z]) vec)  [[x 1] [x 2] [x 1] [x 2]])
(assert= (_>> x [_ _] (for [y _ z [1 2]] [y z]) vec)  [[x 1] [x 2] [x 1] [x 2]])

(assert (nil? (some_> {} :a name)))
(assert (nil? (some_> 1 {:a _} :b name)))
(assert (nil? (some_>> {} :a name)))
(assert (nil? (some_>> 1 {:a _} :b name)))


;; aliases:
(assert-expands-to (ar/_> x [_ _]) (let* [_ x] [_ _]))
(assert (nil? (ar/some_> {} :a [_] first name)))
(assert-expands-to (ar/_>> x [_ _]) (let* [_ x] [_ _]))
(assert (nil? (ar/some_>> {} :a [_] first name)))
