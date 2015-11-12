(ns wiki.components.sphinx
  (:require [clojure.java.jdbc :as clj-jdbc]
            [com.stuartsierra.component :as component])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- connection-pool
  "Create a connection pool for the given database spec."
  [{:keys [subprotocol subname classname user password
           excess-timeout idle-timeout minimum-pool-size maximum-pool-size
           test-connection-query
           idle-connection-test-period
           test-connection-on-checkin
           test-connection-on-checkout
           stringtype]
    :or {excess-timeout (* 30 60)
         idle-timeout (* 3 60 60)
         minimum-pool-size 2
         maximum-pool-size 15
         test-connection-query nil
         idle-connection-test-period 0
         test-connection-on-checkin false
         test-connection-on-checkout false}}]
  {:datasource (doto (ComboPooledDataSource.)
                 (.setDriverClass classname)
                 (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                 (.setUser user)
                 (.setPassword password)
                 (.setMaxIdleTimeExcessConnections excess-timeout)
                 (.setMaxIdleTime idle-timeout)
                 (.setMinPoolSize minimum-pool-size)
                 (.setMaxPoolSize maximum-pool-size)
                 (.setIdleConnectionTestPeriod idle-connection-test-period)
                 (.setTestConnectionOnCheckin test-connection-on-checkin)
                 (.setTestConnectionOnCheckout test-connection-on-checkout)
                 (.setPreferredTestQuery test-connection-query))})

(defrecord Sphinx [config conn]
  component/Lifecycle
  (start [this]
    (if conn
      this
      (assoc this :conn (connection-pool config))))
  (stop [this]
    (if-not conn
      this
      (do (-> conn :datasource (.close))
          this))))

(defn new-sphinx [config]
  (map->Sphinx {:config config}))

(defn conn [component]
  (:conn component))
