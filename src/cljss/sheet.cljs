(ns cljss.sheet
  (:require [goog.object :as gobj]
            [goog.dom :as dom]))

(def ^:private dev? ^boolean goog.DEBUG)
(def ^:private limit 65534)

(defn- make-style-tag []
  (let [tag (dom/createElement "style")
        head (aget (dom/getElementsByTagNameAndClass "head") 0)]
    (gobj/set tag "type" "text/css")
    (dom/appendChild tag (dom/createTextNode ""))
    (dom/appendChild head tag)
    tag))

(defn- find-sheet [tag]
  (if-let [sheet (gobj/get tag "sheet")]
    sheet
    ;; workaround for Firefox
    (let [sheets (gobj/get js/document "styleSheets")]
      (loop [idx 0
             sheet (aget sheets idx)]
        (if (= tag (gobj/get sheet "ownerNode"))
          sheet
          (recur (inc idx) (aget sheets (inc idx))))))))


(defprotocol ISheet
  (insert! [this css cls-name])
  (flush! [this])
  (filled? [this]))

(deftype Sheet [tag cls-names]
  ISheet
  (insert! [this rule cls-name]
    (when (filled? this)
      (throw (js/Error. (str "A stylesheet can only have " limit " rules"))))
    (when-not (@cls-names cls-name)
      (swap! cls-names conj cls-name)
      (let [sheet (find-sheet tag)
            rules-count (gobj/get (gobj/get sheet "cssRules") "length")]
        (if dev?
         (if (not= (.indexOf rule "@import") -1)
           (.insertBefore tag (dom/createTextNode rule) (gobj/get tag "firstChild"))
           (dom/appendChild tag (dom/createTextNode rule)))
         (try
           (if (not= (.indexOf rule "@import") -1)
             (.insertRule sheet rule 0)
             (.insertRule sheet rule rules-count))
           (catch :default e
             (when dev?
               (js/console.warn "Illegal CSS rule" rule))))))))
  (flush! [this]
    (-> tag
        .parentNode
        (.removeChild tag)))
  (filled? [this]
    (= (count @cls-names) limit)))

(defn create-sheet []
  (Sheet. (make-style-tag) (atom #{})))
