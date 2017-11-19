(ns mee6.modules
  "A base namespace that exports the modules abstraction.")

(defprotocol IModule
  (-run [_ local])
  (-check [_ local]))
