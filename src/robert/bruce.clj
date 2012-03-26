(ns robert.bruce
  (:refer-clojure :exclude [double])
  (:import (clojure.lang IObj)))

(defn always
  "default :return? predicate: any return value indicates success"
  [x]
  true)

(def default-options {:sleep 10000
                      :tries 5
                      :decay identity
                      :return? always
                      :catch Exception
                      :error-hook (constantly nil)})

(defn double
  "decay option: 2^x"
  [x]
  (* 2 x))

(defn exponential
  "decay option: e^x"
  [x]
  (* Math/E x))

(defn golden-ratio
  "decay option: φ^x"
  [x]
  (* 1.6180339887 x))

(defn truthy?
  ":return? option: only truthy return values indicate success"
  [x]
  (boolean x))

(defn falsey?
  ":return? option: only falsey return values indicate success"
  [x]
  (not (truthy? x)))

(defn catch
  "internal function that returns a collection of exceptions to catch"
  [options]
  (let [catch (:catch options)]
    (if (coll? catch)
      catch
      [catch])))

(defn resolve-decay
  "internal function that returns a function that implements the
  selected decay strategy, said function will take a number as an
  operand and return a number as a result"
  [options]
  (let [d (:decay options)
        f (cond (nil? d) identity
                (fn? d) d
                (number? d) #(* d %)
                (keyword? d)
                (when-let [f (->> d name symbol (ns-resolve 'robert.bruce))]
                  @f))]
    (if f
      (assoc options :decay f)
      (throw (IllegalArgumentException.
              (str "Unrecognized :decay option: " d))))))

(defn resolve-return
  "internal function that returns a function that implements the
  selected return? criterion"
  [options]
  (let [d (:return? options)
        f (cond (nil? d) always
                (fn? d) d
                (keyword? d)
                (when-let [f (->> d name symbol (ns-resolve 'robert.bruce))]
                  @f))]
    (if f
      (assoc options :return? f)
      (throw (IllegalArgumentException.
              (str "Unrecognized :return? option: " d))))))

(defn init-options
  [options]
  (-> options (assoc :try 1) resolve-decay resolve-return))

(defn parse
  "internal function that parses arguments into usable bits"
  [args]
  (let [argc (count args)
        fn (first (filter fn? args))
        options (if (and (> argc 1)
                         (map? (first args)))
                  (first args)
                  {})
        options (merge default-options
                       (select-keys (meta fn)
                                    (keys default-options))
                       options)
        args (rest (drop-while (complement fn?) args))]
    [options fn args]))

(defn try-again?
  "internal function that determines whether we try again"
  [options t]
  (let [tries (:tries options)]
    (and (some #(isa? (type t) %) (catch options))
         (or (= :unlimited (keyword tries))
             (pos? tries)))))

(defn update-tries
  "internal function that updates the number of tries that remain"
  [options]
  (-> options
      (update-in [:tries] (if (= :unlimited (:tries options))
                            identity
                            dec))
      (update-in [:try] inc)))

(defn update-sleep
  "internal function that updates sleep with the decay function"
  [options]
  (update-in options [:sleep] (if (:sleep options)
                                (:decay options)
                                identity)))

(def ^{:dynamic true} *first-try* nil)
(def ^{:dynamic true} *last-try* nil)
(def ^{:dynamic true} *try* nil)
(def ^{:dynamic true} *error* nil)

(defn retry
  "internal function that will actually retry with the specified options"
  [options f]
  (binding [*try* (:try options)
            *first-try* (= 1 (:try options))
            *last-try* (= 1 (:tries options))
            *error* (::error options)]
    (try
      (let [ret (f)]
        (if (instance? IObj ret)
          (vary-meta ret assoc :tries *try*)
          ret))
      (catch Throwable t
        (let [options (-> options
                          update-tries
                          (assoc ::error t))
              continue ((:error-hook options) t)]
          (if (or (true? continue)
                  (and (not (false? continue))
                       (try-again? options t)))
            (do
              (when-let [sleep (:sleep options)]
                (Thread/sleep (long sleep)))
              #(retry (update-sleep options) f))
            (throw t)))))))


(defn try-try-again
  "if at first you don't succeed, intelligent retry trampolining"
  {:arglists '([fn] [fn & args] [options fn] [options fn & args])}
  [& args]
  (let [[options fn args] (parse args)]
    (trampoline retry options #(apply fn args))))
