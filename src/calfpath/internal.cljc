;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns calfpath.internal
  #?(:cljs (:require-macros calfpath.internal))
  (:require
    [clojure.string :as string])
  #?(:clj (:import
            [java.util Map Map$Entry]
            [calfpath Util])))


(defn expected
  ([expectation found]
    (throw (ex-info
             (str "Expected " expectation ", but found (" (type found) ") " (pr-str found))
             {:found found})))
  ([pred expectation found]
    (when-not (pred found)
      (expected expectation found))))


(defn parse-uri-template
  "Given a URI pattern string, e.g. '/user/:id/profile/:descriptor/' parse it and return a vector of alternating string
  and keyword tokens, e.g. ['/user/' :id '/profile/' :descriptor '/']. The marker char is typically ':'."
  [marker-char ^String pattern]
  (let [[^String path partial?] (if (and (> (count pattern) 1)
                                      (string/ends-with? pattern "*"))
                                  [(subs pattern 0 (dec (count pattern))) true]  ; chop off last char
                                  [pattern false])
        n (count path)
        separator \/]
    (loop [i (int 0) ; current index in the URI string
           j (int 0) ; start index of the current token (string or keyword)
           s? true   ; string in progress? (false implies keyword in progress)
           r []]
      (if (>= i n)
        [(conj r (let [t (subs path j i)]
                   (if s?
                     t
                     (keyword t))))
         partial?]
        (let [^char ch  (get path i)
              [jn s? r] (if s?
                          (if (= ^char marker-char ch)
                            [(unchecked-inc i) false (conj r (subs path j i))]
                            [j true r])
                          (if (= separator ch)
                            [i true  (conj r (keyword (subs path j i)))]
                            [j false r]))]
          (recur (unchecked-inc i) (int jn) s? r))))))


(def ^:const default-separator \:)


(defn as-uri-template
  [uri-pattern-or-template]
  (cond
    (string? uri-pattern-or-template)      (parse-uri-template default-separator uri-pattern-or-template)
    (and (vector? uri-pattern-or-template)
      (every? (some-fn string? keyword?)
        uri-pattern-or-template))          uri-pattern-or-template
    :otherwise                             (expected "a string URI pattern or a parsed URI template"
                                             uri-pattern-or-template)))


(def ^:const uri-match-end-index :calfpath/uri-match-end-index)


(defmacro get-uri-match-end-index
  [request]
  `(or (get ~request uri-match-end-index) 0))


(defmacro assoc-uri-match-end-index
  [request end-index]
  `(assoc ~request uri-match-end-index ~end-index))


(def path-params :calfpath/path-params)


(def valid-method-keys #{:get :head :options :patch :put :post :delete})


(defmacro method-dispatch
  ([method-keyword request expr]
    (when-not (valid-method-keys method-keyword)
      (expected (str "a method key (" valid-method-keys ")") method-keyword))
    (let [method-string (->> (name method-keyword)
                          string/upper-case)
          default-expr {:status 405
                        :headers {"Allow"        method-string
                                  "Content-Type" "text/plain"}
                        :body (str "405 Method not supported. Only " method-string " is supported.")}]
      `(if (identical? ~method-keyword (:request-method ~request))
         ~expr
         ~default-expr)))
  ([method-keyword request expr default-expr]
    (when-not (valid-method-keys method-keyword)
      (expected (str "a method key (" valid-method-keys ")") method-keyword))
    `(if (identical? ~method-keyword (:request-method ~request))
       ~expr
       ~default-expr)))


(defn conj-maps
  "Merge two maps efficiently using conj."
  [old-map new-map]
  (conj
    (if (nil? old-map)
      {}
      old-map)
    new-map))


#?(:cljs (def reduce-mkv reduce-kv)
    :clj (defn reduce-mkv
           "Same as clojure.core/reduce-kv for java.util.Map instances."
           [f init ^Map m]
           (if (or (nil? m) (.isEmpty m))
             init
             (let [i (.iterator (.entrySet m))]
               (loop [last-result init]
                 (if (.hasNext i)
                   (let [^Map$Entry pair (.next i)]
                     (recur (f last-result (.getKey pair) (.getValue pair))))
                   last-result))))))


(defn invoke
  "Invoke first arg as a function on remaing args."
  ([f]            (f))
  ([f x]          (f x))
  ([f x y]        (f x y))
  ([f x y & args] (apply f x y args)))


(defn strip-partial-marker
  [x]
  (when (string? x)
    (if (string/ends-with? ^String x "*")
      (subs x 0 (dec (count x)))
      x)))


;; helpers for `routes -> wildcard trie`


(defn split-routes-having-uri
  "Given mixed routes (vector), split into those having distinct routes and those that don't."
  [routes uri-key]
  (reduce (fn [[with-uri no-uri] each-route]
            (if (and (contains? each-route uri-key)
                  ;; wildcard already? then exclude
                  (not (string/ends-with? ^String (get each-route uri-key) "*")))
              [(conj with-uri each-route) no-uri]
              [with-uri (conj no-uri each-route)]))
    [[] []] routes))


(defn tokenize-routes-uris
  "Given routes with URI patterns, tokenize them as vectors."
  [routes-with-uri uri-key]
  (expected vector?    "a vector of routes" routes-with-uri)
  (expected #(every?
               map? %) "a vector of routes" routes-with-uri)
  (expected some?      "a non-nil URI key"  uri-key)
  (mapv (fn [route]
          (expected map? "a route map" route)
          (let [^String uri-template (get route uri-key)]
            (as-> uri-template $
              (string/split $ #"/")
              (mapv #(if (string/starts-with? ^String % ":")
                       (keyword (subs % 1))
                       %)
                    $)
              (if (string/ends-with? uri-template "/")
                (conj $ "")
                $))))
    routes-with-uri))


(defn find-prefix-tokens
  "Given routes with URI-patterns, find the common (non empty) prefix URI pattern tokens."
  [routes-uri-tokens]
  (reduce (fn [tokens-a tokens-b]
            (->> (map vector tokens-a tokens-b)
              (take-while (fn [[a b]] (= a b)))
              (mapv first)))
    routes-uri-tokens))


(defn dropper
  [n]
  (fn [items] (->> items
                (drop n)
                vec)))


(defn find-prefix-tokens-pair
  "Given routes with URI-patterns, find the common (non empty) prefix URI pattern tokens and balance tokens."
  [routes-uri-tokens]
  (let [prefix-tokens (find-prefix-tokens routes-uri-tokens)]
    (when-not (= [""] prefix-tokens)
      [prefix-tokens (-> (count prefix-tokens)
                       dropper
                       (mapv routes-uri-tokens))])))


(defn find-discriminator-tokens
  [routes-uri-tokens]
  (let [max-cut-count (->> routes-uri-tokens
                        (mapv count)
                        (apply min))
        prefix-tokens (find-prefix-tokens routes-uri-tokens)
        prefix-remain (-> (count prefix-tokens)
                        dropper
                        (mapv routes-uri-tokens))
        delta-tokens  (mapv (comp vector first) prefix-remain)
        suffix-tokens (when true ;(<= (inc (count prefix-tokens)) max-cut-count)
                        (->> prefix-remain
                          (mapv (dropper 1))
                          find-prefix-tokens))
        tok-cut-count (+ (count prefix-tokens) 1 (count suffix-tokens))]
    [(->> delta-tokens
       (mapv #(-> prefix-tokens
                (concat % suffix-tokens)
                vec)))
     tok-cut-count]))


(defn find-discriminator-tokens2
  [routes-uri-tokens]
  (loop [token-count   1
         token-vectors routes-uri-tokens]
    (cond
      (some empty?
        token-vectors)   [nil 0]
      (->> token-vectors
        (map first)
        (apply =))       (recur (inc token-count) (mapv next token-vectors))
      :else              [(->> routes-uri-tokens
                            (mapv #(vec (take token-count %))))
                          token-count])))


(declare triefy-all)


(defn triefy [routes-with-uri ^long trie-threshold uri-key]  ; return vector of routes
  (expected vector?          "vector of routes" routes-with-uri)
  (expected #(every? map? %) "vector of route-maps" routes-with-uri)
  (expected (every-pred
              integer? pos?) "a positive integer" trie-threshold)
  (expected some?            "a non-nil uri-key" uri-key)
  (let [routes-uri-tokens (tokenize-routes-uris routes-with-uri uri-key)  ; [ [t1 t2 ..] [t1 t2 ..] ...]
        [prefix-tokens
         token-vectors]   (find-prefix-tokens-pair routes-uri-tokens)]
    (if (seq prefix-tokens)
      ;; we found a common URI-prefix for all routes
      [{uri-key (-> "/"
                  (string/join prefix-tokens)
                  (str "*"))
        :nested (as-> token-vectors $
                  (mapv (fn [route tokens]
                          (assoc route uri-key (string/join "/" tokens))) routes-with-uri $)
                  (triefy-all $ trie-threshold uri-key))}]
      ;; we need to find URI-prefix groups now
      (let [[first-tokens
             first-count] (find-discriminator-tokens2 routes-uri-tokens)
            token-counts  (->> routes-uri-tokens
                            (group-by #(take first-count %))
                            (reduce-kv #(assoc %1 %2 (count %3)) {}))]
        (if (->> [routes-with-uri routes-uri-tokens first-tokens]
              (map count)
              (apply =))
          (->> [routes-with-uri routes-uri-tokens first-tokens]
            (apply map vector)
            (sort-by last)
            (partition-by last)
            (reduce (fn [result-routes batch]
                      (if (> (count batch) trie-threshold)
                        (let [[sub-prefix-tokens _] (-> (mapv first batch)
                                                      (tokenize-routes-uris uri-key)  ; [ [t1 t2 ..] [t1 t2 ..] ...]
                                                      (find-prefix-tokens-pair))      ; look-ahead prefix tokens
                              prefix-tokens-count   (count sub-prefix-tokens)]
                          (conj result-routes
                                {uri-key (as-> sub-prefix-tokens $
                                           (string/join "/" $)
                                           (str $ "*"))
                                 :nested (as-> batch $
                                           (mapv (fn [[r ts ft]]
                                                   (assoc r uri-key (->> (drop prefix-tokens-count ts)
                                                                      (string/join "/")
                                                                      (str "/")))) $)
                                           (triefy-all $ trie-threshold uri-key))}))
                        (->> batch
                          (mapv first)
                          (into result-routes))))
              []))
          routes-with-uri)))))


(defn triefy-all
  [routes ^long trie-threshold uri-key]
  (expected #(> ^long % 1) "value of :trie-threshold must be more than 1" trie-threshold)
  (let [[with-uri no-uri] (split-routes-having-uri routes uri-key)]
    (if (> (count with-uri) trie-threshold)
      (-> (triefy with-uri trie-threshold uri-key)
        vec
        (into no-uri))
      routes)))


;; ----- URI match -----


(def ^:const NO-PARAMS {})


(def ^:const FULL-MATCH-INDEX -1)


(def FULL-MATCH-NO-PARAMS [NO-PARAMS FULL-MATCH-INDEX])


(defn partial-match
  ([^long end-index]        [NO-PARAMS end-index])
  ([params ^long end-index] [params end-index]))


(defn full-match
  [params]
  [params FULL-MATCH-INDEX])


(defn match-uri
  "Match given URI string against URI pattern, returning a vector `[params-map ^int end-index]` on success,
  and `nil` on no match.

  | Argument               | Description                                       |
  |------------------------|---------------------------------------------------|
  | uri                    | the URI string to match                           |
  | begin-index            | index in the URI string to start matching at      |
  | pattern-tokens         | URI pattern tokens to match against               |
  | attempt-partial-match? | flag to indicate whether to attempt partial-match |"
  [uri ^long begin-index pattern-tokens attempt-partial-match?]
  #?(:cljs (when-not (= begin-index FULL-MATCH-INDEX)  ; if already a full-match then no need to match any further
             (let [token-count (count pattern-tokens)
                   actual-uri  (subs uri begin-index)
                   actual-len  (count actual-uri)]
               (if (= token-count 1)  ; if length==1, then token must be string (static URI path)
                 (let [static-path (first pattern-tokens)
                       static-size (count static-path)]
                   (when (string/starts-with? actual-uri static-path)  ; URI begins with path, so at least partial match exists
                     (if (= (count actual-uri) (count static-path))  ; if full match exists, then return as such
                       FULL-MATCH-NO-PARAMS
                       (when attempt-partial-match?
                         (partial-match static-size)))))
                 (loop [path-params  (transient {})
                        actual-index 0
                        token-index  0]
                   (if (>= token-index token-count)
                     (if (< actual-index actual-len)
                       (when attempt-partial-match?
                         (partial-match (persistent! path-params) actual-index))
                       (full-match (persistent! path-params)))
                     (let [token (get pattern-tokens token-index)]
                       (if (string? token)
                         ;; string token
                         (when (string/starts-with? actual-uri token)
                           (recur path-params (unchecked-add actual-index (count token)) (unchecked-inc token-index)))
                         ;; must be a keyword
                         (let [[u-path-params
                                u-actual-index] (loop [sb (transient [])  ; string buffer
                                                       j actual-index]
                                                  (if (>= j actual-len)  ; 'separator not found' implies URI has ended
                                                    [(assoc! path-params token (apply str (persistent! sb)))
                                                     actual-len]
                                                    (let [ch (get uri j)]
                                                      (if (= \/ ch)
                                                        [(assoc! path-params token (apply str (persistent! sb)))
                                                         j]
                                                        (recur (conj! sb ch) (unchecked-inc j))))))]
                           (recur u-path-params (long u-actual-index) (unchecked-inc token-index))))))))))
      :clj (Util/matchURI uri begin-index pattern-tokens attempt-partial-match?)))
