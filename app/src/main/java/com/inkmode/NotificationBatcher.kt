package com.inkmode

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Holds back notifications and releases them as one digest every 30/60/120 minutes.
 * Calls, alarms, ongoing ones and anything from "always let through" apps are left alone.
 */
class NotificationBatcher : NotificationListenerService() {

    override fun onListenerConnected() {
        Digest.schedule(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!prefs.batching) return
        if (sbn.packageName == packageName) return
        if (sbn.packageName in prefs.letThrough) return
        if (sbn.isOngoing || !sbn.isClearable) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return
        if (n.category in PASS_CATEGORIES) return

        val isSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0
        if (!isSummary) {
            val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) return
            HeldStore.add(this, Held(sbn.key, sbn.packageName, title, text, sbn.postTime), n.contentIntent)
        }
        try { cancelNotification(sbn.key) } catch (_: Exception) {}
    }

    companion object {
        private val PASS_CATEGORIES = setOf(
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_REMINDER,
            Notification.CATEGORY_EVENT,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_STOPWATCH,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_ERROR,
            Notification.CATEGORY_MISSED_CALL,
        )
    }
}
