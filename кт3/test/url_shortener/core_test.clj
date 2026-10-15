(ns url-shortener.core-test
  (:require [clojure.test :refer :all]
            [url-shortener.server :as server]
            [url-shortener.client :as client]
            [url-shortener.db :as db])
  (:import [java.nio.file Files]
           [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]))

(defn call [base method parts]
  (let [url (str base "/" (clojure.string/join "/" (map client/encode parts)))
        req (-> (HttpRequest/newBuilder (URI/create url))
                (.method method (HttpRequest$BodyPublishers/noBody)) .build)
        res (.send (HttpClient/newHttpClient) req (HttpResponse$BodyHandlers/ofString))]
    [(.statusCode res) (.body res)]))

(deftest http-crud-and-persistence
  (let [file (Files/createTempFile "url-shortener-test" ".db" (make-array java.nio.file.attribute.FileAttribute 0))
        path (str file)
        instance (server/start! 0 path)
        base (str "http://localhost:" (-> instance :server .getAddress .getPort))
        normal "https://example.com/a/b?q=hello%20world&x=1+2#fragment"
        updated "https://example.org/new/path?text=Привет&value=%25"]
    (try
      (let [[status short] (call base "POST" [normal])]
        (is (= 201 status))
        (is (re-matches #"[a-f0-9]{10}" short))
        (is (= [200 normal] (call base "GET" [short])))
        (is (= normal (client/request! base "GET" [short])))
        (is (= [200 "OK"] (call base "PUT" [short updated])))
        (is (= [200 updated] (call base "GET" [short])))
        (is (= updated (db/find-url path short)))
        ;; Открытие нового сервера с тем же файлом проверяет сохранение на диске.
        (let [second-instance (server/start! 0 path)
              second-base (str "http://localhost:" (-> second-instance :server .getAddress .getPort))]
          (try (is (= [200 updated] (call second-base "GET" [short])))
               (finally (server/stop! second-instance))))
        (is (= [200 "OK"] (call base "DELETE" [short])))
        (is (= [404 "Ошибка"] (call base "GET" [short])))
        (is (= [404 "Ошибка"] (call base "DELETE" [short])))
        (is (= [404 "Ошибка"] (call base "PUT" [short normal]))))
      (is (= [400 "Ошибка"] (call base "POST" ["not-a-url"])))
      (is (= [400 "Ошибка"] (call base "POST" ["ftp://example.com"])))
      (is (= [400 "Ошибка"] (call base "PUT" ["missing"])))
      (is (= [405 "Ошибка"] (call base "PATCH" ["missing"])))
      (is (= [404 "Ошибка"] (call base "GET" ["' OR 1=1 --"])))
      (is (= "Ошибка" (client/request! base "GET" ["missing"])))
      (finally
        (server/stop! instance)
        (Files/deleteIfExists file)))))
