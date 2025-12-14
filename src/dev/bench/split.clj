(ns bench.split
  (:require
    [clojure.string :as string]
    [starfederation.datastar.clojure.api.common :as c]))


(defn old-datalines [data-lines! prefix text]
  (reduce
    (fn [acc part]
      (conj! acc (str prefix part)))
    data-lines!
    (string/split-lines text)))


(def input "hello there\n wold !\r\n How are \ryou \ntoday")
(def input-big (apply str (repeat 100 input)))

(defn bench [f input]
  (println "---------------------------------------")
  (dotimes [_ 20]
    (time
      (dotimes [_ 10000]
        (f (transient []) "elements " input)))))


(comment
  (bench old-datalines input-big)
  (bench c/add-data-lines! input-big))
