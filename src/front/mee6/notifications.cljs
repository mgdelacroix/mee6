(ns mee6.notifications)

(defn supported?
  []
  (.hasOwnProperty js/window "Notification"))

(defn enabled?
  []
  (#{"granted"} js/Notification.permission))

(defn request-permission
  []
  (if (supported?)
    (js/Notification.requestPermission)
    (js/alert "Your browser doesn't support notifications")))

(defn notify
  [{:keys [title body icon]}]
  (let [default-icon "https://pbs.twimg.com/profile_images/891830444144050176/pNv9n78H.jpg"
        options (clj->js {:body body :icon default-icon})]
    (new js/Notification title options)))
