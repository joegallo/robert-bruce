(ns robert.bruce
  (:refer-clojure :exclude [double])
  (:import (clojure.lang IObj)))

(def default-options {:sleep 10000
                      :tries 5
                      :decay identity
                      :return? :always
                      :catch Exception
                      :error-hook (constantly nil)})

(defn double
  "decay option: 2^n"
  [x]
  (* 2 x))

(defn exponential
  "decay option: e^n"
  [x]
  (* Math/E x))

(defn golden-ratio
  "decay option: φ^n"
  [x]
  (* 1.6180339887 x))

(defn always
  ":return? option (default): any return value indicates success"
  [x]
  true)

(defn truthy?
  ":return? option: only truthy return values indicate success"
  [x]
  (boolean x))

(defn falsey?
  ":return? option: only falsey return values indicate success"
  [x]
  (not x))

(defn resolve-catch
  "internal function that resolves the value of the :catch in options
  to a collection of exceptions to catch"
  [options]
  (let [catch (:catch options)]
    (if (coll? catch)
      catch
      [catch])))

(defn resolve-keyword
  "resolves k as a function named in this namespace"
  [k]
  (when-let [f (->> k name symbol (ns-resolve 'robert.bruce))]
    @f))

(defn resolve-decay
  "internal function that resolves the value of :decay in options to a
  decay strategy: a function that accepts a current time delay
  milliseconds in and returns a new time delay milliseconds"
  [{d :decay :as options}]
  (if-let [d (cond (nil? d) identity
                   (fn? d) d
                   (number? d) #(* d %)
                   (keyword? d) (resolve-keyword d))]
    (assoc options :decay d)
    (throw (IllegalArgumentException.
            (str "Unrecognized :decay option " d)))))

(defn resolve-return
  "internal function that resolves the value of :return? in options to
  a return? filter: a function that accepts a candidate return value
  and returns truthy to approve it being returned or falsey to request
  a retry."
  [{r :return? :as options}]
  (if-let [r (cond (nil? r) always
                   (fn? r) r
                   (keyword? r) (resolve-keyword r))]
    (assoc options :return? r)
    (throw (IllegalArgumentException.
            (str "Unrecognized :return? option " r)))))

(defn init-options
  [options]
  (-> options (assoc :try 1) resolve-decay resolve-return))

(defn parse
  "internal function that parses arguments into usable bits"
  [arg & args]
  (let [[arg-options [f & args]] (if (map? arg)
                                   [arg args]
                                   [nil (cons arg args)])
        meta-options (select-keys (meta f) (keys default-options))
        options (merge default-options meta-options arg-options)]
    [options f args]))

(defn try-again?
  "internal function that determines whether we try again"
  [options error]
  (let [tries (:tries options)]
    (and (or (not (instance? Throwable error))
             (some #(instance? % error) (resolve-catch options)))
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
  "internal function that will actually retry with the specified
  options"
  [options f]
  (binding [*try* (:try options)
            *first-try* (= 1 (:try options))
            *last-try* (= 1 (:tries options))
            *error* (::error options)]
    (let [{:keys [returned thrown]}
          (try
            {:returned (f)}
            (catch Throwable t
              (when (:log-hook options)
                ((:log-hook options) t))
              {:thrown t}))]
      (if (and (not thrown) ((:return? options) returned))
        (if (instance? IObj returned)
          (vary-meta returned assoc :tries *try*)
          returned)
        (let [error (or thrown returned)
              options (-> options
                          update-tries
                          (assoc ::error error))
              continue ((:error-hook options) error)]
          (if (or (true? continue)
                  (and (not (false? continue))
                       (try-again? options error)))
            (do
              (when-let [sleep (:sleep options)]
                (when (pos? sleep)
                  (Thread/sleep (long sleep))))
              #(retry (update-sleep options) f))
            (if thrown
              (throw thrown)
              returned)))))))

(defn try-try-again
  "if at first you don't succeed, intelligent retry trampolining"
  {:arglists '([fn] [fn & args] [options fn] [options fn & args])}
  [arg & args]
  (let [[options fn args] (apply parse arg args)
        options (init-options options)]
    (trampoline retry options #(apply fn args))))
