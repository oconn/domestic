(ns domestic.core-test
  (:require [domestic.core :as sut]
            [domestic.logger :as logger]
            [cljs.test :as t :include-macros true]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [helix.core :refer [defnc $]]
            [helix.hooks :as helix-hooks]))

(def rdom (js/require "react-dom"))
(def JSDOM (.-JSDOM (js/require "jsdom")))
(def testing-library (js/require "@testing-library/react"))
(def render (.-render testing-library))
(def screen (.-screen testing-library))

(defn new-dom [] ^js (new JSDOM "`<!DOCTYPE html><html><body><div id=\"app\"></div></body></html>"
                          #js {:pretendToBeVisual true}))
(defn get-element-by-id [window id] (-> window .-document (.getElementById id)))
(defn get-root-element [window] (get-element-by-id window "app"))

(sut/defdispatcher test-dispatcher {:log-error #(swap! % inc)})

(sut/defevent test-dispatcher :inc
  [state inc-by]
  (if (some? inc-by)
    (swap! state update :counter + inc-by)
    (swap! state update :counter inc)))

(sut/defevent test-dispatcher :multiplier
  [state {:keys [multiplier]} number]
  (reset! state {:counter (* number multiplier)}))

(t/deftest bind-dispatcher
  (let [errors-logged (atom 0)]
    (with-redefs [logger/log-error #(do (swap! errors-logged inc) nil)]
      (t/testing "fails when it is not passed a multimethod dispatch function."
        (t/is (nil? (sut/bind-dispatcher {} {}))))

      (t/testing "logs an error when it is not passed a multimethod dispatch function."
        (t/is (= 1 @errors-logged)))))

  (t/testing "returns a dispatcher when passed a multimethod dispatch function."
    (t/is (fn? (sut/bind-dispatcher test-dispatcher {})))))

(t/deftest defevent
  (let [errors-logged (atom 0)
        dispatch (sut/bind-dispatcher test-dispatcher (atom {:counter 0}))]
    (t/testing "throws an error when a message is dispatched with no corrosponding event"
      (with-redefs [logger/log-error #(do (swap! errors-logged inc) nil)]
        (dispatch [:dec])
        ;; TODO Need to figure out how to redefine within macro
        #_(t/is (= 1 @errors-logged))))

    (t/testing "dispatches the proper event when called correctly."
      (-> (dispatch [:inc])
          :counter
          (= 1)
          t/is))

    (t/testing "properly passes additional args when called with them."
      (-> (dispatch [:inc 5])
          :counter
          (= 6)
          t/is)))

  (let [dispatch (sut/bind-dispatcher test-dispatcher (atom {:counter 0}) {:multiplier 2})]
    (t/testing "proxies additional arguments when bound with them"
      (-> (dispatch [:multiplier 2])
          :counter
          (= 4)
          t/is)

      (-> (dispatch [:multiplier 4])
          :counter
          (= 8)
          t/is)))

  (t/testing "can directly invoke an event by passing the expected arguments"
    (-> (test-dispatcher :inc (atom {:counter 0}))
        :counter
        (= 1)
        t/is)))

(def reagent-window (.-window (new-dom)))
(set! (.-window js/global) reagent-window)

(sut/defdispatcher reagent-dispatcher {})

(sut/defevent reagent-dispatcher :add-user
  [state {:user/keys [id] :as user}]
  (swap! state assoc-in [:users id] user))

(defn- reagent-component
  []
  (let [local-state (reagent/atom {:users {}})
        dispatch (sut/bind-dispatcher reagent-dispatcher local-state)]

    (dispatch [:add-user {:user/id 1 :user/age 34}])
    (dispatch [:add-user {:user/id 2 :user/age 27}])

    (fn []
      [:div
       [:button {:id "addUserBtn"
                 :on-click #(dispatch [:add-user {:user/id (str (random-uuid)) :user/age 67}])}]
       [:ul {:id "reagentUserList"}
        (for [{:user/keys [id age]} (vals (:users @local-state))]
          [:li {:key id} [:p age]])]])))

(t/deftest reagent-example
  (reagent-dom/render
   [reagent-component]
   (get-root-element reagent-window)
   (fn []
     (let [add-user-btn (get-element-by-id reagent-window "addUserBtn")
           get-list #(array-seq (.-childNodes (get-element-by-id reagent-window "reagentUserList")))]
       (t/testing "works properly in form 2 component"
         (t/is (= 2 (count (get-list)))))

       ;; TODO This is not working as expected..
       (t/testing "works when triggered from user events"
         (.click add-user-btn)
         #_(t/is (= 3 (count (get-list))))
         (.click add-user-btn)
         (.click add-user-btn)
         (.click add-user-btn)
         #_(t/is (= 6 (count (get-list)))))))))

(sut/defdispatcher helix-dispatcher {})

(sut/defevent helix-dispatcher :add-user
  [state update-state {:user/keys [id] :as user}]
  (update-state (assoc-in state [:users id] user)))

(def helix-window (.-window (new-dom)))
(set! (.-window js/global) helix-window)
(set! (.-document js/global) (.-document helix-window))

(defnc helix-component-use-state
  []
  (let [[local-state update-state] (helix-hooks/use-state {:users {}})
        dispatch (sut/bind-dispatcher helix-dispatcher local-state update-state)]

    (helix-hooks/use-effect
     :once
     ;; Note calling twice will result in the last one winning in this case
     (dispatch [:add-user {:user/id 3 :user/age 19}]) ;; This user get's overwritten
     (dispatch [:add-user {:user/id 4 :user/age 65}]))

    ($ :div
       ($ :button {:id "addUserBtn"
                   :on-click #(dispatch [:add-user {:user/id (str (random-uuid)) :user/age 67}])})
       ($ :ul {:id "helixUserList"}
          (for [{:user/keys [id age]} (vals (:users local-state))]
            ($ :li {:key id} ($ :p age)))))))

(t/deftest helix-example-use-state
  (render ($ helix-component-use-state))
  (let [add-user-btn (get-element-by-id helix-window "addUserBtn")
        get-list #(array-seq (.-childNodes (get-element-by-id helix-window "helixUserList")))]
    (t/testing "works properly in hooks"
      (t/is (= 1 (count (get-list)))))

    (t/testing "works when triggered from user events"
      (.click add-user-btn)
      (t/is (= 2 (count (get-list))))
      (.click add-user-btn)
      (.click add-user-btn)
      (.click add-user-btn)
      (t/is (= 5 (count (get-list)))))))

(defnc helix-component-use-reducer
  []
  (let [[local-state update-state] (helix-hooks/use-reducer #(merge-with merge %1 %2) {:users {}})
        dispatch (sut/bind-dispatcher helix-dispatcher local-state update-state)]

    (helix-hooks/use-effect
     :once
     (dispatch [:add-user {:user/id 3 :user/age 19}])
     (dispatch [:add-user {:user/id 4 :user/age 65}]))

    ($ :div
       ($ :button {:id "addUserBtn2"
                   :on-click #(dispatch [:add-user {:user/id (str (random-uuid)) :user/age 67}])})
       ($ :ul {:id "helixUserList2"}
          (for [{:user/keys [id age]} (vals (:users local-state))]
            ($ :li {:key id} ($ :p age)))))))

(t/deftest helix-example-use-reducer
  (render ($ helix-component-use-reducer))
  (let [add-user-btn (get-element-by-id helix-window "addUserBtn2")
        get-list #(array-seq (.-childNodes (get-element-by-id helix-window "helixUserList2")))]
    (t/testing "works properly in hooks"
      (t/is (= 2 (count (get-list)))))

    (t/testing "works when triggered from user events"
      (.click add-user-btn)
      (t/is (= 3 (count (get-list))))
      (.click add-user-btn)
      (.click add-user-btn)
      (.click add-user-btn)
      (t/is (= 6 (count (get-list)))))))
