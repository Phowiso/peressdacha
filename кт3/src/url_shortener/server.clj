(ns url-shortener.server
  (:require [clojure.string :as str]
            [url-shortener.db :as db])
  (:import [com.sun.net.httpserver HttpServer HttpHandler]
           [java.net InetSocketAddress URI URLDecoder]
           [java.nio.charset StandardCharsets]
           [java.util.concurrent Executors]))

(defn valid-url? [value]
  (try
    (let [uri (URI. value)]
      (and (contains? #{"http" "https"} (some-> (.getScheme uri) str/lower-case))
           (not (str/blank? (.getHost uri)))))
    (catch Exception _ false)))

(defn decode [value] (URLDecoder/decode value "UTF-8"))

(defn route [path method raw-path]
  ;; Разделяем только на две части: обычный URL может содержать слеши.
  (let [route-path (subs raw-path 1)]
    (case method
      "POST" (let [normal (decode route-path)]
               (if (valid-url? normal)
                 [201 (db/create! path normal)]
                 [400 "Ошибка"]))
      "GET" (if-let [normal (db/find-url path (decode route-path))]
              [200 normal] [404 "Ошибка"])
      "PUT" (let [[short normal] (str/split route-path #"/" 2)
                  normal (when normal (decode normal))]
              (cond
                (or (str/blank? short) (nil? normal) (not (valid-url? normal))) [400 "Ошибка"]
                (db/update! path (decode short) normal) [200 "OK"]
                :else [404 "Ошибка"]))
      "DELETE" (if (db/delete! path (decode route-path)) [200 "OK"] [404 "Ошибка"])
      [405 "Ошибка"])))

(defn start! [port path]
  (db/initialize! path)
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" (int port)) 0)
        executor (Executors/newFixedThreadPool 8)]
    (.createContext server "/"
      (reify HttpHandler
        (handle [_ exchange]
          (try
            (let [[status body] (try
                                  (route path (.getRequestMethod exchange)
                                         (.getRawPath (.getRequestURI exchange)))
                                  (catch IllegalArgumentException _ [400 "Ошибка"])
                                  (catch Exception e
                                    (binding [*out* *err*] (println "Ошибка сервера:" (.getMessage e)))
                                    [500 "Ошибка"]))
                  bytes (.getBytes ^String body StandardCharsets/UTF_8)]
              (.set (.getResponseHeaders exchange) "Content-Type" "text/plain; charset=utf-8")
              (.sendResponseHeaders exchange status (alength bytes))
              (with-open [output (.getResponseBody exchange)] (.write output bytes)))
            (finally (.close exchange))))))
    (.setExecutor server executor)
    (.start server)
    {:server server :executor executor}))

(defn stop! [{:keys [server executor]}]
  (.stop server 0)
  (.shutdownNow executor))
