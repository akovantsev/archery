## Archery

`_>` `some_>` threading macros for clojure.


### installation

```clojure
;; in deps.edn
{:deps {github-akovantsev/archery
        {:git/url "https://github.com/akovantsev/archery"
         :sha     "somegitsha"}}}
```

```clojure
(ns foo.bar.baz
  (:require 
   [com.akovantsev.archery :refer [some_> _>]]))
```

Or, if you are feeling extra adventurous:
```clojure
(ns foo.bar.baz
  (:refer-clojure :exclude [-> ->> some-> some->>])
  (:require
   [com.akovantsev.archery :refer [some_> _>]
    :rename {_> ->, some_> some->}]))
```

### some_>

Same as `some->` for `->` but for `_>`. All rules and gotchas are the same as for `_>`.

### _>
Essentially, `_>` is a bunch of clojure.core's `as->` and `->` wrapped into top level `->`.
<br>
<br>For each top-level-arg-form:
<br>If arg is `(_> ...)` form or any form **without** `_` – it receives prev form as first arg, as if inside clojure.core `->`<sup>*</sup>.
<br>If arg **contains** `_` symbol – it gets wrapped into clojure.core `as->`: `(as-> _ (some [arg form {containing _}]))`.
<br>Then `_>` wraps all top-level-arg-forms with clojure.core `->`.
<br>That's it.

```clojure
(_> x foo (bar a) (baz _ b _) (_> _ lel) :quux)
;=>
(clojure.core/-> x
  (foo ,)                             ;; has no _ - gets prev as first arg
  (bar , a)                           ;; has no _ - gets prev as first arg
  (clojure.core/as-> _ (baz _ b _))   ;; has _ and is not _> - gets wrapped with as->
  (_> , _ lel)                        ;; is _>    - gets prev as first arg
  (:quux ,))                          ;; has no _ - gets prev as first arg
```


<br><sup>*</sup>There was a different design option:
<br>Thread as `->` only into scalars (numbers, keywords, symbols, etc), `as->` into forms with parens or with `_`.
<br>It would have forced you to specify `_` in all fn calls, which would have:
- saved you some arity-related bugs,
<br>but
- it would have prevented several cool features, like `threading into try-catch` and reader-macro getters and spy.
- and IDE should already hint you about arity error, effectively pushing you to explicitly add `_` to forms with parens. 


### Why
1. `->` `->>` `some->` `some->>` `cond->` `cond->>` – notice how many already, **oof** 
2. They implicitly put prev form into next, which is same amount of letters as in onion nested form, but readable, **nice**
3. However each has its own fixed place where prev from goes. This means you need to 1) plan your arrows beforehand, and 2) often rewrite when adding expressions later, **oof**
4. You can nest `->>`kin inside `->`kin, but not vise versa, makes rewrites and planning even more unpleasant, **oof**
5. This generates larger git diffs, **oof**
6. This increases nestedness and indentation level, however, conceptually you are still threading, **oof**
7. They wrap "scalar" forms-without-parens with parens, e.g. `(-> x f)` -> `(-> x (f))` -> `(f x)`, **nice**
8. None of those above can thread into some middle arg position `(f a _ b)`, but `as->` can, **nice**
9. But that's another one to plan and rewrite, **oof**
10. And it can't be transitioned into from `->>`kin, ***oof*
11. So maybe lets just use only `as->`? But it requires you to always specify placeholder. Tedious, but it is good for readability and maintenance, **ok**
12. But it can't auto-wrap scalars with parens, and what is THAT? `(as-> 1 _ inc) => #object[clojure.core$inc]`, **big oof**
13. Brevity is very important:`(-> x foo bar baz)` vs `(as-> x _ (foo _) (bar _) (baz _)`, **oof** again
14. However it can fork `(as-> x _ [_ _])`, **nice**
15. `cond->`kin can't `else` **oof** and thread into predicates, **oof**
16. But `as->` can do both: `(as-> x _ (if (pred _) (then _) (else _))`, **nice**, **nice**
17. It can sideeffects: `(as-> x _ (do (print _) _))`, **nice**
18. Other prior art on the internet: `when->` `arg->` `for->` `when->` `let->` `fn->` `-<>` `-?<>` `-<><:p` `-?>` `-?>>` `.?.` :DDD that's 12 **oofs** just to match amount, imagine combinatorial explosion while trying to compose em, **OOF**
19. Designing and memorizing which function fits which macro: `->` or `->>`?, **oof**
20. Trying to fit expr into `->` or `->>` by changing args order by wrapping partial-call-expressions in lambdas, **oof** (but remember, you can't nest `#()`, **oof**)
21. Forgetting change `(or x)` when refactoring from `(-> x foo bar (or y))` to `(->> x (foo a) bar (or y))`, **oof**
22. `->` and `->>` can't thread into coll literals to wrap things: `(-> x [])` is `([] x)` not `[x]`, **oof**
23. Only `as->` can thread into coll literals: `(as-> x _ [{:a _}])`  => `[{:a x}]`, **nice**, but is rarely used due to overall verbocity, **oof** 

### What

1. Autowrapping scalars with parens, **nice**, `(_> x foo bar baz :a)`
2. Threading as `->` by default, preserving at least some implicitness, **nice**
3. Default fixed standard placeholder `_`, no need to come up with new one and type it out everytime, **nice**
4. Can't change it though, **ok**
5. Autowrapping form containing `_` with `as->`, so entire form works like giant `as->`:
6. Gives us forking `(_> x [_ _])`, **nice**
7. Gives us `else`, **nice**, `dynamic preds`, **nice**, `(_> x (if (pred _) #{_} [_])`
8. Sideeffects `(_> x (do (print _) _))`, **nice**
9. Any arg position, **nice**, and any number of arg positions, **nice**, `(_> x (f a b c _ d _))`
10. No more need for 666 other, barely composable threading macros, **nice**
11. No need to wrap in lambdas just to adjust arg positions for current `->` or `->>`, **nice**
12. Explicit `(or _ default)`, **nice**
13. No need in extra nesting and indentation, just keep threading! **nice**
14. No need for plumbing planning and rewriting when pipeline steps change, **nice**
15. Collecting checkpoints a la reductions with forking and nested `_>`: `(_> 1 inc [_ (_> _ (+ 2) (* 3)])  => [2 12]`, **nice**
16. `(/ _ x)` `(< x _ y)` are now explicit and don't suffer from `->` to `->>` refactorings, just like `(or _ x)`, **nice**
17. All those extra `_` after inline map lambdas `(map long-ass-multi-line-lambda _)`, **oof**
18. Threadable `for`! `(_> x (for [a _] [a _ a]) ...)`, **nice**
19. Threadable `reduce` whichever way you want! `(_> x (reduce f _ ys) ...)`, `(_> x (reduce f r _) ...)`, **nice**
20. Wrapping with literals `(_> x [:a _])` => `[:a x]` now *literally* looks like its output, **nice!**
21. You might think, the only missing thing now is threadable unwrapping which looks like input... I got you: https://github.com/akovantsev/destruct  

### Bonus

```clojure
;; data-reader.clj
{g com.akovantsev.archery/data-reader-for-getter}
```
```clojure
(_> {"a" {\b [:c "d"]}} #g"a" #g\b (group-by keyword? _) #g true #g 0)
;=>
(_>
  {"a" {\b [:c "d"]}}
  ((com.akovantsev.archery/-mg "a"))
  ((com.akovantsev.archery/-mg \b))
  (group-by keyword? _)
  ((com.akovantsev.archery/-mg true))
  ((com.akovantsev.archery/-mg 0)))
;=>
:c
```


### Gotchas

#### Explicitly put _ in non-trivial forms

#### Don't forget forms containing _ no longer receive _ as first arg implicitly.

#### Nesting
No behavior inheritance between `_>` `some_>`, just like between core `->` and `some->`: inner `->` is opaque for outer `some->`, and vice versa.
```clojure
(_> nil
  (some_> name) ;; short circuit before name throws NullPointerException
  (/ 0))  ;; but proceeds right into Divide by zero
```


#### Old habits 
Watch out for "ignore bind" `_` inside bindings inside `_>` 
```clojure
(clojure.walk/macroexpand-all
  '(_> [1 2 3] (let [[x y _] _] [x y _]) str/join))
   ;;  1                  2  1       2  
;=>
(str/join
  (let* [_ [1 2 3]]
    (let* [vec__23224 _
           x          (clojure.core/nth vec__23224 0 nil)
           y          (clojure.core/nth vec__23224 1 nil)
           _          (clojure.core/nth vec__23224 2 nil)] ;;overridden
      [x y _])))
;=>
"123"
;  2

;;vs:

(clojure.walk/macroexpand-all
  '(_> [1 2 3] (let [[x y _z] _] [x y _]) str/join))
   ;;  1                  2   1       1
;=>
(str/join
  (let* [_ [1 2 3]]
    (let* [vec__23248 _
           x          (clojure.core/nth vec__23248 0 nil)
           y          (clojure.core/nth vec__23248 1 nil)
           _z         (clojure.core/nth vec__23248 2 nil)]
      [x y _])))
;=>
"12[1 2 3]"
;  1
```


#### Threading as `->` vs. `some->` as `as->`:

Only `->` always expands "thread-first". It is easy to forget `as->` and `some->` expand defferently. 
```clojure
;; core -> expands this with everything inside of try, wicked but handy!

(clojure.walk/macroexpand-all 
  '(-> 1 str inc (try (catch Exception e :caught))))
;;  ^                ^
;=>
(try
  (inc (str 1))
  (catch Exception e :caught))
;=>
:caught


;; but core some-> and as-> expand this outside, rendering try - noop:

(clojure.walk/macroexpand-all
  '(some-> 1 str inc (try (catch Exception e :caught))))
;;  ^                   ^
(let* [G__2306 1
       G__2306 (if (nil? G__2306) nil (str G__2306))
       G__2306 (if (nil? G__2306) nil (inc G__2306))]
  ;;                                    ^ throws outside of try
  (if (clojure.core/nil? G__2306)
    nil
    (try
      G__2306
      (catch Exception e :caught))))


(clojure.walk/macroexpand-all
  '(as-> 1 _ (str _) (inc _) (try _ (catch Exception e :caught))))
;;  ^                             ^
;=>
(let* [_ 1
       _ (str _)
       _ (inc _)] ;;throws here
  (try _ (catch Exception e :caught)))
;=>
ClassCastException: java.lang.String cannot be cast to class java.lang.Number



;; recall, _> and some_> wrap form in as-> only if it contains _,
;; so this behaves like core -> (note NO _ inside try):

(clojure.walk/macroexpand-all
  '(_> 1 str inc (try (catch Exception e :caught))))
;;                   ^
;=>
(try
  (inc (str 1))
  (catch Exception e :caught))
;=>
:caught

;; and this behaves just like core as-> (note _ inside try):

(clojure.walk/macroexpand-all
  '(_> 1 str inc (try _ (catch Exception e :caught))))
;;                    ^
;=>
(-> 1 
  str
  inc  ;;throws here
  (as-> _ (try _ (catch Exception e :caught)))
;=>
(as-> (inc (str 1)) _        ;;throws here 
  (try _ (catch Exception e :caught)))
;=>
(let* [_ (inc (str 1))]      ;;throws here
  (try _ (catch Exception e :caught)))
;=>
ClassCastException
```