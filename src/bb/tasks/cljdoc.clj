(ns tasks.cljdoc
  (:require
   [babashka.fs    :as fs]
   [babashka.tasks :as t]
   [clojure.string :as string]
   [tasks.build    :as build]))


(def cljdoc-dir ".cljdoc-preview")

(defn home-dir [] (str (fs/home)))
(defn cwd      [] (str (fs/cwd)))

(defn git-rev []
  (-> (t/shell {:out :string} "git" "rev-parse" "HEAD")
      :out
      string/trim))


(defn try-docker-cmd [cmd]
  (try
    (t/shell {:out :string} cmd "--help")
    cmd
    (catch Exception _ nil)))


(def docker-cmd
  (or (try-docker-cmd "docker")
      (try-docker-cmd "podman")))


(defn start-server! []
  (fs/create-dirs cljdoc-dir)

  (t/shell
    docker-cmd "run"
    "--rm"
    "--publish" "8000:8000"
    "--volume" (str (home-dir) "/.m2:/root/.m2")
    "--volume" "./.cljdoc-preview:/app/data"
    "--platform" "linux/amd64"
    "cljdoc/cljdoc"))


(def libs
  #{:sdk
    :brotli
    :aleph
    :aleph-malli-schemas
    :http-kit
    :http-kit-malli-schemas
    :malli-schemas
    :ring
    :ring-malli-schemas})

(defn lib-arg->kw [lib]
  (cond-> lib
          (and (string? lib) (string/starts-with? lib ":"))
          (subs 1)

          true
          keyword))


(defn ingest! [lib & {:keys [version]
                      :or {version (build/current-version)}}]
  (let [lib (lib-arg->kw lib)]
    (if (contains? libs lib)
      (t/shell
        docker-cmd "run"
        "--rm"
        "--volume" (str (home-dir) "/.m2:/root/.m2")
        "--volume" (str (cwd) ":/repo-to-import")
        "--volume" "./.cljdoc-preview:/app/data"
        "--platform" "linux/amd64"
        "--entrypoint" "clojure"
        "cljdoc/cljdoc" "-Sforce" "-M:cli" "ingest"
        "--project" (str "dev.data-star.clojure/" (name lib))
        "--version" (str version)
        "--git" "/repo-to-import"
        "--rev" (git-rev))
      (println "Can't ingest " lib ", unrecognized"))))


(defn clean! []
  (fs/delete-tree cljdoc-dir))
