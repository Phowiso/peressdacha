(ns url-shortener.core
  (:gen-class)
  (:require [url-shortener.server :as server]
            [url-shortener.client :as client]))

(defn -main [& args]
  (try
    (cond
      (empty? args) (client/run-client! "http://localhost:8080")
      (= "client" (first args)) (client/run-client! (or (second args) "http://localhost:8080"))
      :else
      (let [port (Integer/parseInt (first args))
            path (or (second args) "url-shortener.db")]
        (when-not (<= 1 port 65535) (throw (IllegalArgumentException. "Порт должен быть от 1 до 65535")))
        (let [instance (server/start! port path)]
          (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable #(server/stop! instance)))
          (println (str "Сервер запущен: http://localhost:" port))
          (println "База данных:" path)
          (println "Для остановки нажмите Ctrl+C."))))
    (catch Exception e
      (binding [*out* *err*]
        (println "Ошибка запуска:" (.getMessage e)))
      (System/exit 1))))
