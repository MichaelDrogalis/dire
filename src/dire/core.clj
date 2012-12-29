(ns dire.core)

(def task-handlers (atom {}))

(defmacro defhandler [handler-name exception-type handler-fn]
  `(swap! task-handlers
          (fn [handlers#]
            (assoc handlers#
              ~handler-name
              (assoc (get handlers# ~handler-name {}) ~exception-type ~handler-fn)))))

(defn supervise [handler-name & args]
  (try
    (apply handler-name args)
    (catch Exception e
      (let [handler (get (get @task-handlers handler-name) (type e) #(println "Untrapped exception: " e))]
        (handler e args)))))

