(ns cljss.rum
  (:require [cljss.core :refer [->styled]]))

(defmacro defstyled [var tag styles]
  (let [[tag# id# static# vals# attrs#] (->styled tag styles)
        create-element# `#(apply js/React.createElement ~tag# (cljs.core/clj->js %1) (sablono.core/html %2))]
    `(def ~var
       (cljss.rum/styled ~id# ~static# ~vals# ~attrs# ~create-element#))))