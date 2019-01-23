(defproject me.dryewo/hystrix-plus "0.1.0"
  :description "Stack trace enhancer for hystrix-clj."
  :url "https://github.com/dryewo/hystrix-plus"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.netflix.hystrix/hystrix-clj "1.5.18" :scope "provided"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]
            [io.aviso/pretty "0.1.35"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]
                                  [io.aviso/pretty "0.1.36"]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s/\\\\[me\\.dryewo\\\\/hystrix-plus \"[0-9.]*\"\\\\]/[me\\.dryewo\\\\/hystrix-plus \"${:version}\"]/" "README.md"]}
  :java-source-paths ["src"]
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
