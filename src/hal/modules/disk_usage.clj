(ns hal.modules.disk-usage)

(defn run
  [session ctx]
  (let [capacity 479610056
        used 301760240
        percentage (/ (* used 100) capacity)]
    {:capacity capacity
     :used used
     :percentage percentage})
  ;; {:error "this shit failed"
  ;;  :stdout ""
  ;;  :stderr ""}
  )

(defn check
  "Check if the result is an error or not, and if the returned
  information triggers any notification"
  [{:keys [percentage]} {:keys [threshold]}]

  ;; error control

  (if (> percentage threshold)
    :red
    :green))
