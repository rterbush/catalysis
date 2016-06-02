(ns catalysis.client.events)


;; ## Top level event handlers

;; Dispatch on event-id
(defmulti comms-handler
  [component comms-msg]
  (:id comms-msg))

(defmethod comms-handler :default ; Fallback
  [_ {:as ev-msg :keys [event]}]
  (js/console.log "Unhandled event: %s" (pr-str event)))

;; Total fucking dev hack; Seems :first-open? is malfunctioning? Have to do with figwheel?
;; Have to figure this out... This is lame
(defonce opened? (atom false))

(defmethod comms-handler :chsk/state
  [component {:as ev-msg :keys [?data]}]
  (js/console.log "chsk/state data:" (pr-str ?data))
  (if (and (:first-open? ?data) (not @opened?))
    (do (js/console.log "Channel socket successfully established!")
        (js/console.log "Sending bootstrap request")
        (chsk-send! [:datsync.client/bootstrap nil])
        (reset! (:open? component) true))
    (js/console.log "Channel socket state change: %s" (pr-str ?data))))

;; Set up push message handler

; Dispatch on event key which is 1st elem in vector
(defmulti push-msg-handler first)

(defmethod handler :chsk/recv
  [app {:as ev-msg :keys [?data]}]
  (push-msg-handler ?data))


;; ## Push message handlers

(defmethod push-msg-handler :datsync/tx-data
  [[_ tx-data]]
  (js/console.log "tx-data recieved:" (pr-str (take 30 tx-data)) "...")
  (datsync/apply-remote-tx! db/conn tx-data))

(defmethod push-msg-handler :datsync.client/bootstrap
  [[_ tx-data]]
  ;; Possibly flag some state somewhere saying bootstrap has taken place?
  (js/console.log "Recieved bootstrap")
  (datsync/apply-remote-tx! db/conn tx-data))

;; TODO Add any custom handlers here!


