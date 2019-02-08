;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns calfpath.long-perf-test
  "Performance benchmarks for long (OpenSensors) routing table."
  (:require
    [clojure.pprint :as pp]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [ataraxy.core   :as ataraxy]
    [bidi.ring      :as bidi]
    [compojure.core :refer [defroutes rfn routes context GET POST PUT ANY]]
    [clout.core     :as l]
    [reitit.ring    :as reitit]
    [calfpath.core  :as cp :refer [->uri ->method ->get ->head ->options ->put ->post ->delete]]
    [calfpath.internal :as i]
    [calfpath.route :as r]
    [citius.core    :as c]))


(use-fixtures :once
  (c/make-bench-wrapper
    ["Reitit" "CalfPath-core-macros" "CalfPath-route-walker" "CalfPath-route-unroll"]
    {:chart-title "Reitit/CalfPath"
     :chart-filename (format "bench-large-routing-table-clj-%s.png" c/clojure-version-str)}))


(defn handler
  [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "OK"})


(defn p-handler
  [params]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (apply str params)})


(defmacro fnpp
  [params]
  (i/expected vector? "params vector" params)
  `(fn [{{:keys ~params} :path-params}]
     (p-handler ~params)))


(def handler-reitit
  (reitit/ring-handler
    (reitit/router
      [["/v2/whoami"                                           {:get handler}]
       ["/v2/users/:user-id/datasets"                          {:get (fnpp [user-id])}]
       ["/v2/public/projects/:project-id/datasets"             {:get (fnpp [project-id])}]
       ["/v1/public/topics/:topic"                             {:get (fnpp [topic])}]
       ["/v1/users/:user-id/orgs/:org-id"                      {:get (fnpp [user-id org-id])}]
       ["/v1/search/topics/:term"                              {:get (fnpp [term])}]
       ["/v1/users/:user-id/invitations"                       {:get (fnpp [user-id])}]
       ["/v1/orgs/:org-id/devices/:batch/:type"                {:get (fnpp [org-id batch type])}]
       ["/v1/users/:user-id/topics"                            {:get (fnpp [user-id])}]
       ["/v1/users/:user-id/bookmarks/followers"               {:get (fnpp [user-id])}]
       ["/v2/datasets/:dataset-id"                             {:get (fnpp [dataset-id])}]
       ["/v1/orgs/:org-id/usage-stats"                         {:get (fnpp [org-id])}]
       ["/v1/orgs/:org-id/devices/:client-id"                  {:get (fnpp [org-id client-id])}]
       ["/v1/messages/user/:user-id"                           {:get (fnpp [user-id])}]
       ["/v1/users/:user-id/devices"                           {:get (fnpp [user-id])}]
       ["/v1/public/users/:user-id"                            {:get (fnpp [user-id])}]
       ["/v1/orgs/:org-id/errors"                              {:get (fnpp [org-id])}]
       ["/v1/public/orgs/:org-id"                              {:get (fnpp [org-id])}]
       ["/v1/orgs/:org-id/invitations"                         {:get (fnpp [org-id])}]
       ;;["/v2/public/messages/dataset/bulk"                     {:get handler}]
       ;;["/v1/users/:user-id/devices/bulk"                      {:get (fnpp [user-id])}]
       ["/v1/users/:user-id/device-errors"                     {:get (fnpp [user-id])}]
       ["/v2/login"                                            {:get handler}]
       ["/v1/users/:user-id/usage-stats"                       {:get (fnpp [user-id])}]
       ["/v2/users/:user-id/devices"                           {:get (fnpp [user-id])}]
       ["/v1/users/:user-id/claim-device/:client-id"           {:get (fnpp [user-id client-id])}]
       ["/v2/public/projects/:project-id"                      {:get (fnpp [project-id])}]
       ["/v2/public/datasets/:dataset-id"                      {:get (fnpp [dataset-id])}]
       ;;["/v2/users/:user-id/topics/bulk"                       {:get (fnpp [user-id])}]
       ["/v1/messages/device/:client-id"                       {:get (fnpp [client-id])}]
       ["/v1/users/:user-id/owned-orgs"                        {:get (fnpp [user-id])}]
       ["/v1/topics/:topic"                                    {:get (fnpp [topic])}]
       ["/v1/users/:user-id/bookmark/:topic"                   {:get (fnpp [user-id topic])}]
       ["/v1/orgs/:org-id/members/:user-id"                    {:get (fnpp [org-id user-id])}]
       ["/v1/users/:user-id/devices/:client-id"                {:get (fnpp [user-id client-id])}]
       ["/v1/users/:user-id"                                   {:get (fnpp [user-id])}]
       ["/v1/orgs/:org-id/devices"                             {:get (fnpp [org-id])}]
       ["/v1/orgs/:org-id/members"                             {:get (fnpp [org-id])}]
       ["/v1/orgs/:org-id/members/invitation-data/:user-id"    {:get (fnpp [org-id user-id])}]
       ["/v2/orgs/:org-id/topics"                              {:get (fnpp [org-id])}]
       ["/v1/whoami"                                           {:get handler}]
       ["/v1/orgs/:org-id"                                     {:get (fnpp [org-id])}]
       ["/v1/users/:user-id/api-key"                           {:get (fnpp [user-id])}]
       ["/v2/schemas"                                          {:get handler}]
       ["/v2/users/:user-id/topics"                            {:get (fnpp [user-id])}]
       ["/v1/orgs/:org-id/confirm-membership/:token"           {:get (fnpp [org-id token])}]
       ["/v2/topics/:topic"                                    {:get (fnpp [topic])}]
       ["/v1/messages/topic/:topic"                            {:get (fnpp [topic])}]
       ["/v1/users/:user-id/devices/:client-id/reset-password" {:get (fnpp [user-id client-id])}]
       ["/v2/topics"                                           {:get handler}]
       ["/v1/login"                                            {:get handler}]
       ["/v1/users/:user-id/orgs"                              {:get (fnpp [user-id])}]
       ["/v2/public/messages/dataset/:dataset-id"              {:get (fnpp [dataset-id])}]
       ["/v1/topics"                                           {:get handler}]
       ["/v1/orgs"                                             {:get handler}]
       ["/v1/users/:user-id/bookmarks"                         {:get (fnpp [user-id])}]
       ["/v1/orgs/:org-id/topics"                              {:get (fnpp [org-id])}]])))


(defn handler-calfpath [request]
  (cp/->uri request
    "/v2/whoami"                                           []                  (cp/->get request (handler request) nil)
    "/v2/users/:user-id/datasets"                          [user-id]           (cp/->get request (handler request) nil)
    "/v2/public/projects/:project-id/datasets"             [project-id]        (cp/->get request (handler request) nil)
    "/v1/public/topics/:topic"                             [topic]             (cp/->get request (handler request) nil)
    "/v1/users/:user-id/orgs/:org-id"                      [user-id org-id]    (cp/->get request (handler request) nil)
    "/v1/search/topics/:term"                              [term]              (cp/->get request (handler request) nil)
    "/v1/users/:user-id/invitations"                       [user-id]           (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/devices/:batch/:type"                [org-id batch type] (cp/->get request (handler request) nil)
    "/v1/users/:user-id/topics"                            [user-id]           (cp/->get request (handler request) nil)
    "/v1/users/:user-id/bookmarks/followers"               [user-id]           (cp/->get request (handler request) nil)
    "/v2/datasets/:dataset-id"                             [dataset-id]        (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/usage-stats"                         [org-id]            (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/devices/:client-id"                  [org-id client-id]  (cp/->get request (handler request) nil)
    "/v1/messages/user/:user-id"                           [user-id]           (cp/->get request (handler request) nil)
    "/v1/users/:user-id/devices"                           [user-id]           (cp/->get request (handler request) nil)
    "/v1/public/users/:user-id"                            [user-id]           (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/errors"                              [org-id]            (cp/->get request (handler request) nil)
    "/v1/public/orgs/:org-id"                              [org-id]            (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/invitations"                         [org-id]            (cp/->get request (handler request) nil)
    ;;"/v2/public/messages/dataset/bulk"                     []                  (cp/->get request (handler request) nil)
    ;;"/v1/users/:user-id/devices/bulk"                      [user-id]           (cp/->get request (handler request) nil)
    "/v1/users/:user-id/device-errors"                     [user-id]           (cp/->get request (handler request) nil)
    "/v2/login"                                            []                  (cp/->get request (handler request) nil)
    "/v1/users/:user-id/usage-stats"                       [user-id]           (cp/->get request (handler request) nil)
    "/v2/users/:user-id/devices"                           [user-id]           (cp/->get request (handler request) nil)
    "/v1/users/:user-id/claim-device/:client-id"           [user-id client-id] (cp/->get request (handler request) nil)
    "/v2/public/projects/:project-id"                      [project-id]        (cp/->get request (handler request) nil)
    "/v2/public/datasets/:dataset-id"                      [dataset-id]        (cp/->get request (handler request) nil)
    ;;"/v2/users/:user-id/topics/bulk"                       [user-id]           (cp/->get request (handler request) nil)
    "/v1/messages/device/:client-id"                       [client-id]         (cp/->get request (handler request) nil)
    "/v1/users/:user-id/owned-orgs"                        [user-id]           (cp/->get request (handler request) nil)
    "/v1/topics/:topic"                                    [topic]             (cp/->get request (handler request) nil)
    "/v1/users/:user-id/bookmark/:topic"                   [user-id topic]     (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/members/:user-id"                    [org-id user-id]    (cp/->get request (handler request) nil)
    "/v1/users/:user-id/devices/:client-id"                [user-id client-id] (cp/->get request (handler request) nil)
    "/v1/users/:user-id"                                   [user-id]           (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/devices"                             [org-id]            (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/members"                             [org-id]            (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/members/invitation-data/:user-id"    [org-id user-id]    (cp/->get request (handler request) nil)
    "/v2/orgs/:org-id/topics"                              [org-id]            (cp/->get request (handler request) nil)
    "/v1/whoami"                                           []                  (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id"                                     [org-id]            (cp/->get request (handler request) nil)
    "/v1/users/:user-id/api-key"                           [user-id]           (cp/->get request (handler request) nil)
    "/v2/schemas"                                          []                  (cp/->get request (handler request) nil)
    "/v2/users/:user-id/topics"                            [user-id]           (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/confirm-membership/:token"           [org-id token]      (cp/->get request (handler request) nil)
    "/v2/topics/:topic"                                    [topic]             (cp/->get request (handler request) nil)
    "/v1/messages/topic/:topic"                            [topic]             (cp/->get request (handler request) nil)
    "/v1/users/:user-id/devices/:client-id/reset-password" [user-id client-id] (cp/->get request (handler request) nil)
    "/v2/topics"                                           []                  (cp/->get request (handler request) nil)
    "/v1/login"                                            []                  (cp/->get request (handler request) nil)
    "/v1/users/:user-id/orgs"                              [user-id]           (cp/->get request (handler request) nil)
    "/v2/public/messages/dataset/:dataset-id"              [dataset-id]        (cp/->get request (handler request) nil)
    "/v1/topics"                                           []                  (cp/->get request (handler request) nil)
    "/v1/orgs"                                             []                  (cp/->get request (handler request) nil)
    "/v1/users/:user-id/bookmarks"                         [user-id]           (cp/->get request (handler request) nil)
    "/v1/orgs/:org-id/topics"                              [org-id]            (cp/->get request (handler request) nil)
    nil))


(defmacro fnp
  [params]
  (i/expected vector? "params vector" params)
  `(fn [{:keys ~params}]
     (p-handler ~params)))


(def opensensors-calfpath-routes
  [{:uri "/v2/whoami"                                           :method :get :handler handler}
   {:uri "/v2/users/:user-id/datasets"                          :method :get :handler (fnp [user-id])}
   {:uri "/v2/public/projects/:project-id/datasets"             :method :get :handler (fnp [project-id])}
   {:uri "/v1/public/topics/:topic"                             :method :get :handler (fnp [topic])}
   {:uri "/v1/users/:user-id/orgs/:org-id"                      :method :get :handler (fnp [user-id org-id])}
   {:uri "/v1/search/topics/:term"                              :method :get :handler (fnp [term])}
   {:uri "/v1/users/:user-id/invitations"                       :method :get :handler (fnp [user-id])}
   {:uri "/v1/orgs/:org-id/devices/:batch/:type"                :method :get :handler (fnp [org-id batch type])}
   {:uri "/v1/users/:user-id/topics"                            :method :get :handler (fnp [user-id])}
   {:uri "/v1/users/:user-id/bookmarks/followers"               :method :get :handler (fnp [user-id])}
   {:uri "/v2/datasets/:dataset-id"                             :method :get :handler (fnp [dataset-id])}
   {:uri "/v1/orgs/:org-id/usage-stats"                         :method :get :handler (fnp [org-id])}
   {:uri "/v1/orgs/:org-id/devices/:client-id"                  :method :get :handler (fnp [org-id client-id])}
   {:uri "/v1/messages/user/:user-id"                           :method :get :handler (fnp [user-id])}
   {:uri "/v1/users/:user-id/devices"                           :method :get :handler (fnp [user-id])}
   {:uri "/v1/public/users/:user-id"                            :method :get :handler (fnp [user-id])}
   {:uri "/v1/orgs/:org-id/errors"                              :method :get :handler (fnp [org-id])}
   {:uri "/v1/public/orgs/:org-id"                              :method :get :handler (fnp [org-id])}
   {:uri "/v1/orgs/:org-id/invitations"                         :method :get :handler (fnp [org-id])}
   ;;{:uri "/v2/public/messages/dataset/bulk"                     :method :get :handler handler}
   ;;{:uri "/v1/users/:user-id/devices/bulk"                      :method :get :handler (fnp [user-id])}
   {:uri "/v1/users/:user-id/device-errors"                     :method :get :handler (fnp [user-id])}
   {:uri "/v2/login"                                            :method :get :handler handler}
   {:uri "/v1/users/:user-id/usage-stats"                       :method :get :handler (fnp [user-id])}
   {:uri "/v2/users/:user-id/devices"                           :method :get :handler (fnp [user-id])}
   {:uri "/v1/users/:user-id/claim-device/:client-id"           :method :get :handler (fnp [user-id client-id])}
   {:uri "/v2/public/projects/:project-id"                      :method :get :handler (fnp [project-id])}
   {:uri "/v2/public/datasets/:dataset-id"                      :method :get :handler (fnp [dataset-id])}
   ;;{:uri "/v2/users/:user-id/topics/bulk"                       :method :get :handler (fnp [user-id])}
   {:uri "/v1/messages/device/:client-id"                       :method :get :handler (fnp [client-id])}
   {:uri "/v1/users/:user-id/owned-orgs"                        :method :get :handler (fnp [user-id])}
   {:uri "/v1/topics/:topic"                                    :method :get :handler (fnp [topic])}
   {:uri "/v1/users/:user-id/bookmark/:topic"                   :method :get :handler (fnp [user-id topic])}
   {:uri "/v1/orgs/:org-id/members/:user-id"                    :method :get :handler (fnp [org-id user-id])}
   {:uri "/v1/users/:user-id/devices/:client-id"                :method :get :handler (fnp [user-id client-id])}
   {:uri "/v1/users/:user-id"                                   :method :get :handler (fnp [user-id])}
   {:uri "/v1/orgs/:org-id/devices"                             :method :get :handler (fnp [org-id])}
   {:uri "/v1/orgs/:org-id/members"                             :method :get :handler (fnp [org-id])}
   {:uri "/v1/orgs/:org-id/members/invitation-data/:user-id"    :method :get :handler (fnp [org-id user-id])}
   {:uri "/v2/orgs/:org-id/topics"                              :method :get :handler (fnp [org-id])}
   {:uri "/v1/whoami"                                           :method :get :handler handler}
   {:uri "/v1/orgs/:org-id"                                     :method :get :handler (fnp [org-id])}
   {:uri "/v1/users/:user-id/api-key"                           :method :get :handler (fnp [user-id])}
   {:uri "/v2/schemas"                                          :method :get :handler handler}
   {:uri "/v2/users/:user-id/topics"                            :method :get :handler (fnp [user-id])}
   {:uri "/v1/orgs/:org-id/confirm-membership/:token"           :method :get :handler (fnp [org-id token])}
   {:uri "/v2/topics/:topic"                                    :method :get :handler (fnp [topic])}
   {:uri "/v1/messages/topic/:topic"                            :method :get :handler (fnp [topic])}
   {:uri "/v1/users/:user-id/devices/:client-id/reset-password" :method :get :handler (fnp [user-id client-id])}
   {:uri "/v2/topics"                                           :method :get :handler handler}
   {:uri "/v1/login"                                            :method :get :handler handler}
   {:uri "/v1/users/:user-id/orgs"                              :method :get :handler (fnp [user-id])}
   {:uri "/v2/public/messages/dataset/:dataset-id"              :method :get :handler (fnp [dataset-id])}
   {:uri "/v1/topics"                                           :method :get :handler handler}
   {:uri "/v1/orgs"                                             :method :get :handler handler}
   {:uri "/v1/users/:user-id/bookmarks"                         :method :get :handler (fnp [user-id])}
   {:uri "/v1/orgs/:org-id/topics"                              :method :get :handler (fnp [org-id])}])


(def compiled-calfpath-routes (r/compile-routes opensensors-calfpath-routes {:show-uris-400? false}))


(def handler-calfpath-route-walker
  (partial r/dispatch compiled-calfpath-routes))


(def handler-calfpath-route-unroll
  (r/make-dispatcher compiled-calfpath-routes))


(defmacro test-compare-perf
  [bench-name & exprs]
  `(do
     (is (= ~@exprs) ~bench-name)
     (when-not (System/getenv "BENCH_DISABLE")
       (c/compare-perf ~bench-name ~@exprs))))


(deftest test-no-match
  (testing "no URI match"
    (let [request {:request-method :get
                   :uri "/hello/joe/"}]
      (test-compare-perf "no URI match"
        (handler-reitit request)
        (handler-calfpath request) (handler-calfpath-route-walker request) (handler-calfpath-route-unroll request))))
  (testing "no method match"
    (let [request {:request-method :post
                   :uri "/v1/orgs/1234/topics"}]
      (test-compare-perf "no method match"
        (handler-reitit request)
        (handler-calfpath request) (handler-calfpath-route-walker request) (handler-calfpath-route-unroll request)))))
