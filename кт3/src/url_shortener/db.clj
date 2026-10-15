(ns url-shortener.db
  (:import [java.sql DriverManager]
           [java.util UUID]))

(defn connection [path]
  (Class/forName "org.sqlite.JDBC")
  (let [conn (DriverManager/getConnection (str "jdbc:sqlite:" path))]
    (with-open [stmt (.createStatement conn)]
      (.execute stmt "PRAGMA busy_timeout = 5000"))
    conn))

(defn initialize! [path]
  (with-open [conn (connection path)
              stmt (.createStatement conn)]
    (.execute stmt "CREATE TABLE IF NOT EXISTS urls (short TEXT PRIMARY KEY, normal TEXT NOT NULL)")))

(defn create! [path normal]
  ;; UUID без дефисов даёт короткий идентификатор; конфликт обрабатывается БД.
  (loop []
    (let [short (subs (.replace (str (UUID/randomUUID)) "-" "") 0 10)
          inserted (with-open [conn (connection path)
                               stmt (.prepareStatement conn "INSERT OR IGNORE INTO urls(short, normal) VALUES (?, ?)")]
                     (.setString stmt 1 short)
                     (.setString stmt 2 normal)
                     (.executeUpdate stmt))]
      (if (pos? inserted) short (recur)))))

(defn find-url [path short]
  (with-open [conn (connection path)
              stmt (.prepareStatement conn "SELECT normal FROM urls WHERE short = ?")]
    (.setString stmt 1 short)
    (with-open [rs (.executeQuery stmt)]
      (when (.next rs) (.getString rs "normal")))))

(defn update! [path short normal]
  (with-open [conn (connection path)
              stmt (.prepareStatement conn "UPDATE urls SET normal = ? WHERE short = ?")]
    (.setString stmt 1 normal)
    (.setString stmt 2 short)
    (pos? (.executeUpdate stmt))))

(defn delete! [path short]
  (with-open [conn (connection path)
              stmt (.prepareStatement conn "DELETE FROM urls WHERE short = ?")]
    (.setString stmt 1 short)
    (pos? (.executeUpdate stmt))))
