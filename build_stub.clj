(require
 '[clojure.string :as str]
 '[clojure.tools.build.api :as b]
 '[clojure.edn :as edn])

(def root-project (-> (edn/read-string (slurp "../deps.edn"))
                      :aliases :neil :project))
(def repo-url-prefix (:url root-project))
(def scm (:scm root-project))
(def project (-> (edn/read-string (slurp "deps.edn"))
                 :aliases :neil :project))
(def cwd (-> (java.io.File. ".")  .getCanonicalFile .getName))
(def rev (str/trim (b/git-process {:git-args "rev-parse HEAD"})))
(def lib (:name project))
(def version (:version project))
(def description (:description project))
(assert lib ":name must be set in deps.edn under the :neil alias")
(assert version ":version must be set in deps.edn under the :neil alias")
(assert description ":description must be set in deps.edn under the :neil alias")

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn permalink [subpath]
  (str repo-url-prefix "/blob/" rev "/" subpath))

(defn jar [_]

  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src/main" "resources"]
                :pom-data [[:description description]
                           [:url (permalink cwd)]
                           [:licenses
                            [:license
                             [:name "The MIT License"]
                             [:url (permalink "LICENSE.md")]]]
                           (conj scm [:tag (str "v" version)])]})
  (b/copy-dir {:src-dirs ["src/main"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (b/install {:basis basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
