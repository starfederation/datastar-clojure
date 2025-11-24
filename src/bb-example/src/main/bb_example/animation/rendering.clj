(ns bb-example.animation.rendering
  (:require
    [bb-example.animation.core :as animation]
    [bb-example.common         :as c]
    [clojure.java.io           :as io]
    [clojure.math              :as math]
    [hiccup2.core              :as h]
    [starfederation.datastar.clojure.api :as d*]))

;; -----------------------------------------------------------------------------
;; Rendering util
;; -----------------------------------------------------------------------------
(defn color-level [distance]
  (case (int distance)
    0 100
    1 50
    2 20
    0))


(defn modify-color ^long [x intensity]
  (animation/clamp-color (- x intensity)))


(defn compute-intensity [ping pixel-position]
  (let [ping-traveled-distance (:traveled ping)
        cell-to-ping-distance (animation/distance pixel-position (:pos ping))
        delta (-> (- ping-traveled-distance cell-to-ping-distance)
                  abs math/floor int)]
    (color-level delta)))


(defn cell-color [state pos]
  (let [!r (volatile! 255)
        !g (volatile! 255)
        !b (volatile! 255)]
      (doseq [ping (:pings state)]
        (let [intensity (compute-intensity ping pos)]
          (when (pos? intensity)
            (case (:color ping)
              :r (do (vswap! !g modify-color intensity)
                     (vswap! !b modify-color intensity))
              :g (do (vswap! !r modify-color intensity)
                     (vswap! !b modify-color intensity))
              :b (do (vswap! !r modify-color intensity)
                     (vswap! !g modify-color intensity))))))
      [@!r @!g @!b]))


;; -----------------------------------------------------------------------------
;; Page generation
;; -----------------------------------------------------------------------------
(def css (slurp (io/resource "bb_example/animation/style.css")))

(defn rgb [r g b]
  (str "rgb(" r ", " g ", " b")"))


(defn cell-style [v]
  (str "background-color: "v";"))


(def on-click
  "@get(`/ping/${event.srcElement.id}`)")


(defn pseudo-pixel [state x y]
  (let [c  (cell-color state (animation/point x y))
        id (str "px-" x "-" y)]
    (h/html
      [:div.pseudo-pixel {:style (cell-style (apply rgb c))
                          :id id}
       " "])))

(defn grid-style [state]
  (let [columns (-> state :size :y)]
    (str "grid-template-columns: repeat(" columns ", 1fr)")))


(defn pseudo-canvas [state]
  (let [size (:size state)
        rows (:x size)
        columns (:y size)
        !pc (volatile! (transient [:div.pseudo-canvas {:style (grid-style state)
                                                       :data-on:click on-click}]))]
    (doseq [r (range 1 (inc rows))
            c (range 1 (inc columns))]
      (vswap! !pc conj! (pseudo-pixel state r c)))
    (persistent! @!pc)))


(defn left-pane [state]
  (h/html
    [:div#left-pane
     (pseudo-canvas state)]))



(defn controls [state]
  (h/html
    [:div
     [:h3 "Controls"]
     [:ul.h-list
      [:li [:button {:data-on:click (d*/sse-get "/refresh")} "refresh"]]
      [:li [:button {:data-on:click (d*/sse-get "/reset")} "reset"]]
      [:li [:button {:data-on:click (d*/sse-get "/random-10")} "add 10"]]
      [:li [:button {:data-on:click (d*/sse-get "/step1")} "step1"]]
      (if (:animator state)
        (h/html
          [:li [:button {:data-on:click (d*/sse-get "/pause")} "pause"]])
        (h/html
          [:li [:button {:data-on:click (d*/sse-get "/play")} "play"]]))]

     [:ul.h-list {:data-signals:rows    (-> state :size :x)
                  :data-signals:columns (-> state :size :y)}
      [:li "rows: "    [:input {:type "number"
                                :data-bind "rows"
                                :data-on:change (d*/sse-post "/resize")}]]
      [:li "columns: " [:input {:type "number"
                                :data-bind "columns"
                                :data-on:change (d*/sse-post "/resize")}]]]]))


(defn log-pane [state]
  (h/html
    [:div#log-pane.stack
     [:h3 "State"]
     [:div.stack
      [:h4 "General state"]
      [:pre (pr-str (:size state))]
      [:span "clock: " (:clock state)]
      [:span "animator:" (:animator state)]]

     (let [pings (:pings state)]
       (when (seq pings)
         [:div
          [:h4 "Pings"]
          [:table
           [:thead
            [:tr
             [:th "clock"] [:th "color"] [:th "duration"] [:th "speed"]
             [:th "traveled"] [:th "pos"]]]
           [:tbody
            (for [ping pings]
              (h/html
                [:tr
                 [:td (:clock ping)]
                 [:td (:color ping)]
                 [:td (:duration ping)]
                 [:td (:speed ping)]
                 [:td (:traveled ping)]
                 [:td [:pre (pr-str (:pos ping))]]]))]]]))]))



(defn right-pane [state]
  (h/html
    [:div#right-pane.stack.center
     (controls state)
     (log-pane state)]))


(defn content [state]
  (h/html
    [:div#main-content.center
     (left-pane state)
     (right-pane state)]))


(defn page [state]
  (h/html
    (c/page-scaffold
      [:div {:data-init (d*/sse-get "/updates")}
       [:style (h/raw css)]
       [:h2.center "Babashka Test"]
       (content state)])))


(defn render-content [state]
  (str (h/html (content state))))
