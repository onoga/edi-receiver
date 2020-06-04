(ns edi.receiver.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [edi.common.app :as app]
            [edi.common.db.jdbc :as db]
            [edi.receiver.api.core :as api]
            [edi.receiver.backend.core :as backend]
            [edi.receiver.saver :as saver]
            [edi.receiver.stats :as stats]
            [edi.receiver.upstream :as upstream]))


(defn- run-app! [options config]
  (log/debug "Options:" options)
  (let [db      (db/connect config)
        backend (backend/create config)
        context {:config   config
                 :upstream (upstream/create (:upstream config))
                 :db       db
                 :backend  backend
                 :stats    (stats/create {:config config
                                          :db db})}]
    (if (saver/run-tests context)
      (let [server (api/start (:api config) context)]
        (-> (Runtime/getRuntime)
            (.addShutdownHook (Thread. #(do (api/stop server)
                                            (db/close db))))))
      (do (log/error "TESTS FAILED")
          (db/close db)
          (backend/close backend)
          (System/exit 1)))))


(defn -main [& args]
  (app/start! args
              [[nil "--sync" "download schemas and tests even if cached"]]
              run-app!))