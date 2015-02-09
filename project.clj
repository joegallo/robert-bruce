(defproject robert/bruce "0.8.0-SNAPSHOT"
  :description "trampolining retries for clojure"
  :url "https://github.com/joegallo/robert-bruce"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha5"]]}}
  :aliases {"all" ["with-profile" ~(apply str (interpose ":" ["dev,1.3"
                                                              "dev,1.4"
                                                              "dev,1.5"
                                                              "dev"
                                                              "dev,1.7"]))]})
