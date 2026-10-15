(defproject url-shortener "0.1.0"
  :description "Служба сокращения URL: HTTP-сервер и терминальный клиент"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.xerial/sqlite-jdbc "3.36.0.3"]]
  :main url-shortener.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "url-shortener-0.1.0-standalone.jar")
