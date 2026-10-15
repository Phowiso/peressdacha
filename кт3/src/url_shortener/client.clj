(ns url-shortener.client
  (:require [clojure.string :as str])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.time Duration]))

(defn encode [value] (URLEncoder/encode value "UTF-8"))

(defn request! [base method parts]
  (try
    (let [http (-> (HttpClient/newBuilder) (.connectTimeout (Duration/ofSeconds 5)) .build)
          uri (URI/create (str (str/replace base #"/+$" "") "/" (str/join "/" (map encode parts))))
          request (-> (HttpRequest/newBuilder uri)
                      (.timeout (Duration/ofSeconds 10))
                      (.method method (HttpRequest$BodyPublishers/noBody)) .build)
          response (.send http request (HttpResponse$BodyHandlers/ofString))]
      (if (<= 200 (.statusCode response) 299) (.body response) "Ошибка"))
    (catch Exception _ "Ошибка")))

(defn prompt [text]
  (println text)
  (flush)
  (read-line))

(defn run-client! [base]
  (println "Клиент URL Shortener")
  (loop []
    (println "\n=== URL Shortener ===\n1. Создать\n2. Показать\n3. Изменить\n4. Удалить\n5. Выйти")
    (let [choice (prompt "Выберите действие:")]
      (cond
        (or (nil? choice) (contains? #{"5" "Выйти"} choice)) (println "До свидания!")
        :else
        (let [operation (case choice
                          ("1" "Создать") ["POST" (some-> (prompt "Введите обычный URL:") vector)]
                          ("2" "Показать") ["GET" (some-> (prompt "Введите короткий URL (идентификатор):") vector)]
                          ("3" "Изменить") ["PUT" (some-> (prompt "Введите короткий и обычный URL через пробел:")
                                                         (str/split #"\s+" 2))]
                          ("4" "Удалить") ["DELETE" (some-> (prompt "Введите короткий URL (идентификатор):") vector)]
                          nil)]
          (if operation
            (let [[method parts] operation
                  expected (if (= method "PUT") 2 1)]
              (println "Ответ:" (if (and (= expected (count parts)) (every? (complement str/blank?) parts))
                                  (request! base method parts) "Ошибка")))
            (println "Неизвестное действие."))
          (recur))))))
