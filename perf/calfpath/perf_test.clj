(ns calfpath.perf-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [compojure.core :refer [defroutes rfn routes context GET POST PUT ANY]]
    [clout.core     :as l]
    [calfpath.core  :refer [->uri ->method ->get ->head ->options ->put ->post ->delete]]
    [citius.core    :as c]))


(defroutes handler-compojure
  (context "/user/:id/profile/:type/" [id type]
    (GET "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "1.1"})
    (PUT "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "1.2"})
    (ANY "/" request {:status 405
                      :headers {"Allow" "GET, PUT"
                                "Content-Type" "text/plain"}
                      :body "405 Method not supported. Supported methods are: GET, PUT"}))
  (context "/user/:id/permissions"    [id]
    (GET "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "2.1"})
    (PUT "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "2.2"})
    (ANY "/" request {:status 405
                      :headers {"Allow" "GET, PUT"
                                "Content-Type" "text/plain"}
                      :body "405 Method not supported. Supported methods are: GET, PUT"}))
  (context "/company/:cid/dept/:did"  [cid did]
    (PUT "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "3"})
    (ANY "/" request {:status 405
                      :headers {"Allow" "PUT"
                                "Content-Type" "text/plain"}
                      :body "405 Method not supported. Only PUT is supported."}))
  (context "/this/is/a/static/route"  []
    (PUT "/" request {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "4"})
    (ANY "/" request {:status 405
                      :headers {"Allow" "PUT"
                                "Content-Type" "text/plain"}
                      :body "405 Method not supported. Only PUT is supported."}))
  (rfn request {:status 400
                :headers {"Content-Type" "text/plain"}
                :body "400 Bad request. URI does not match any available uri-template."}))


(let [uri-1 (l/route-compile "/user/:id/profile/:type/")
      uri-2 (l/route-compile "/user/:id/permissions/")
      uri-3 (l/route-compile "/company/:cid/dept/:did/")
      uri-4 (l/route-compile "/this/is/a/static/route")]
  (defn handler-clout
    [request]
    (condp l/route-matches request
      uri-1 (let [{:keys [id type]} request]
                                        (case (:request-method request)
                                          :get {:status 200
                                                :headers {"Content-Type" "text/plain"}
                                                :body "1.1"}
                                          :put {:status 200
                                                :headers {"Content-Type" "text/plain"}
                                                :body "1.2"}
                                          {:status 405
                                           :headers {"Allow" "GET, PUT"
                                                     "Content-Type" "text/plain"}
                                           :body "405 Method not supported. Supported methods are: GET, PUT"}))
      uri-2 (let [{:keys [id]} request]
                                      (case (:request-method request)
                                        :get {:status 200
                                              :headers {"Content-Type" "text/plain"}
                                              :body "2.1"}
                                        :put {:status 200
                                              :headers {"Content-Type" "text/plain"}
                                              :body "2.2"}
                                        {:status 405
                                         :headers {"Allow" "GET, PUT"
                                                   "Content-Type" "text/plain"}
                                         :body "405 Method not supported. Supported methods are: GET, PUT"}))
      uri-3 (let [{:keys [cid did]} request]
                                        (if (identical? :put (:request-method request))
                                          {:status 200
                                           :headers {"Content-Type" "text/plain"}
                                           :body "3"}
                                          {:status 405
                                           :headers {"Allow" "PUT"
                                                     "Content-Type" "text/plain"}
                                           :body "405 Method not supported. Only PUT is supported."}))
      uri-4 (let []
                                       (if (identical? :put (:request-method request))
                                         {:status 200
                                          :headers {"Content-Type" "text/plain"}
                                          :body "4"}
                                         {:status 405
                                          :headers {"Allow" "PUT"
                                                    "Content-Type" "text/plain"}
                                          :body "405 Method not supported. Only PUT is supported."}))
      {:status 400
       :headers {"Content-Type" "text/plain"}
       :body "400 Bad request. URI does not match any available uri-template."})))


(defn handler-calfpath
  [request]
  (->uri request
    "/user/:id/profile/:type/" [id type] (->method request
                                           :get {:status 200
                                                 :headers {"Content-Type" "text/plain"}
                                                 :body "1.1"}
                                           :put {:status 200
                                                 :headers {"Content-Type" "text/plain"}
                                                 :body "1.2"})
    "/user/:id/permissions/"   [id]      (->method request
                                           :get {:status 200
                                                 :headers {"Content-Type" "text/plain"}
                                                 :body "2.1"}
                                           :put {:status 200
                                                 :headers {"Content-Type" "text/plain"}
                                                 :body "2.2"})
    "/company/:cid/dept/:did/" [cid did] (->put request {:status 200
                                                         :headers {"Content-Type" "text/plain"}
                                                         :body "3"})
    "/this/is/a/static/route"  []        (->put request {:status 200
                                                         :headers {"Content-Type" "text/plain"}
                                                         :body "4"})))


(use-fixtures :once (c/make-bench-wrapper ["Compojure" "Clout" "CalfPath"]
                      {:chart-title "Compojure/Clout/CalfPath"
                       :chart-filename (format "bench-clj-%s.png" c/clojure-version-str)}))


(defmacro test-compare-perf
  [bench-name & exprs]
  `(do
     (is (= ~@exprs) ~bench-name)
     (c/compare-perf ~bench-name ~@exprs)))


(deftest test-no-match
  (testing "no URI match"
    (let [request {:request-method :get
                   :uri "/hello/joe/"}]
      (test-compare-perf "no URI match" (handler-compojure request) (handler-clout request) (handler-calfpath request))))
  (testing "no method match"
    (let [request {:request-method :put
                   :uri "/user/1234/profile/compact/"}]
      (test-compare-perf "no method match" (handler-compojure request) (handler-clout request) (handler-calfpath request)))))


(deftest test-match
  (testing "static route match"
    (let [request {:request-method :put
                   :uri "/this/is/a/static/route"}]
      (test-compare-perf "static URI match, 1 method" (handler-compojure request) (handler-clout request) (handler-calfpath request))))
  (testing "pattern route match"
    (let [request {:request-method :get
                   :uri "/user/1234/profile/compact/"}]
      (test-compare-perf "pattern URI match, 2 methods" (handler-compojure request) (handler-clout request) (handler-calfpath request)))
    (let [request {:request-method :get
                   :uri "/company/1234/dept/5678/"}]
      (test-compare-perf "pattern URI match, 1 method" (handler-compojure request) (handler-clout request) (handler-calfpath request)))))
