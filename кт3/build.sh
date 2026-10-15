#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
lein deps
lein test
lein uberjar
echo 'Сборка завершена!'
echo 'Сервер: java -jar target/url-shortener-0.1.0-standalone.jar 8080'
echo 'Клиент: java -jar target/url-shortener-0.1.0-standalone.jar'
