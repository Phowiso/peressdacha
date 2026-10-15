(def game (atom nil))

(defn start [from to]
  (if (> from to)
    "Начало диапазона должно быть не больше конца."
    (do
      (reset! game {:from from
                    :to to
                    :guess (quot (+ from to) 2)})
      "Я готов...")))

(defn guess-my-number []
  (if-let [state @game]
    (:guess state)
    "Сначала вызови (start от до)."))

(defn smaller []
  (if-let [{:keys [from guess]} @game]
    (let [to (dec guess)]
      (if (> from to)
        "В диапазоне не осталось чисел. Проверь подсказки."
        (do
          (swap! game assoc
                 :to to
                 :guess (quot (+ from to) 2))
          (guess-my-number))))
    "Сначала вызови (start от до)."))

(defn bigger []
  (if-let [{:keys [to guess]} @game]
    (let [from (inc guess)]
      (if (> from to)
        "В диапазоне не осталось чисел. Проверь подсказки."
        (do
          (swap! game assoc
                 :from from
                 :guess (quot (+ from to) 2))
          (guess-my-number))))
    "Сначала вызови (start от до)."))
