(ns robert.bruce
  (:refer-clojure :exclude [double]))

(def default-options {:sleep 10000
                      :tries 5
                      :decay identity
                      :catch Exception})

(defn double [x]
  (* 2 x))

(defn exponential [x]
  (* Math/E x))

(defn golden-ratio [x]
  (* 1.6180339887 x))

(defn catch
  "internal function that returns a collection of exceptions to catch"
  [options]
  (let [catch (:catch options)]
    (if (coll? catch)
      catch
      [catch])))

(defn decay
  "internal function that returns of a function that implements the selected
decay strategy, said function will take a number as an operand and return a
number as a result"
  [options]
  (let [d (:decay options)]
    (cond (nil? d) identity
          (fn? d) d
          (number? d) #(* d %)
          (keyword? d) @(ns-resolve 'robert.bruce (symbol (name d))))))

(defn parse
  "internal function that parses arguments into usable bits"
  [args]
  (let [argc (count args)
        fn (first (filter fn? args))
        options (if (and (> argc 1)
                         (map? (first args)))
                  (first args)
                  {})
        options (merge default-options options)
        args (rest (drop-while (complement fn?) args))]
    [options fn args]))

(defn try-again?
  "internal function that determines whether we try again"
  [options t]
  (let [tries (:tries options)]
    (and (some #(isa? (type t) %) (catch options))
         (or (= :unlimited (keyword tries))
             (pos? tries)))))

(defn update-tries [options]
  "internal function that updates the number of tries that remain"
  (update-in options [:tries] (if (= :unlimited (:tries options))
                                identity
                                dec)))

(defn update-sleep [options]
  "internal function that updates sleep with the decay function"
  (update-in options [:sleep] (if (:sleep options)
                                (decay options)
                                identity)))

(defn retry
  "internal function that will actually retry with the specified options"
  [options f]
  (try
    (f)
    (catch Throwable t
      (let [options (update-tries options)]
        (if (try-again? options t)
          (do
            (when-let [sleep (:sleep options)]
              (Thread/sleep (long sleep)))
            #(retry (update-sleep options) f))
          (throw t))))))

(defn try-try-again
  "if at first you don't succeed, intelligent retry trampolining"
  {:arglists '([fn] [fn & args] [options fn] [options fn & args])}
  [& args]
  (let [[options fn args] (parse args)]
    (trampoline retry options #(apply fn args))))
