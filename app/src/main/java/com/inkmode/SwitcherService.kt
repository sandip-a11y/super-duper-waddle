package com.inkmode

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process

/**
 * Checks which app is in front about once a second while the screen is on, and switches
 * grayscale off in colour apps. It stops checking while the screen is off.
 */
class SwitcherService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usm: UsageStatsManager
    private var current: String? = null
    private var lastTs = 0L
    private var polling = false

    private val poll = object : Runnable {
        override fun run() {
            check()
            if (polling) handler.postDelayed(this, POLL_MS)
        }
    }

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> startPolling()
                Intent.ACTION_SCREEN_OFF -> stopPolling()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        usm = getSystemService(UsageStatsManager::class.java)
        startForeground(NOTIF_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        registerReceiver(screen, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, Context.RECEIVER_NOT_EXPORTED)
        // Look back a minute so we know the current app straight away.
        lastTs = System.currentTimeMillis() - 60_000
        if (getSystemService(PowerManager::class.java).isInteractive) startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPolling()
        try { unregisterReceiver(screen) } catch (_: Exception) {}
        instance = null
        // Don't leave the phone stuck in grayscale inside a colour app.
        Grayscale.set(this, false, force = true)
        super.onDestroy()
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        handler.post(poll)
    }

    private fun stopPolling() {
        polling = false
        handler.removeCallbacks(poll)
    }

    private fun check() {
        val now = System.currentTimeMillis()
        // Overlap a little: events can be logged slightly after they happen.
        val events = try { usm.queryEvents(lastTs - 2_000, now) } catch (_: Exception) { return }
        val ev = UsageEvents.Event()
        var latest: String? = null
        var latestTs = lastTs
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED && ev.timeStamp > latestTs) {
                latest = ev.packageName
                latestTs = ev.timeStamp
            }
        }
        lastTs = maxOf(latestTs, now - 2_000)
        if (latest != null && latest != current) {
            current = latest
            apply()
        } else if (current == null) {
            apply()
        }
    }

    fun apply() {
        val prefs = Prefs(this)
        val pkg = current
        Grayscale.set(this, prefs.enabled && (pkg == null || pkg !in prefs.colourApps))
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Colour switch running", NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Inkmode is switching colour per app")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val POLL_MS = 1_000L
        private const val CHANNEL = "switcher"
        private const val NOTIF_ID = 1

        @Volatile var instance: SwitcherService? = null

        fun hasUsageAccess(ctx: Context): Boolean {
            val ops = ctx.getSystemService(AppOpsManager::class.java)
            return ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }

        /** Starts the service if it can do its job. Safe to call often. */
        fun startIfReady(ctx: Context) {
            if (instance != null) return
            if (!Prefs(ctx).enabled || !hasUsageAccess(ctx) || !Grayscale.hasAccess(ctx)) return
            try {
                ctx.startForegroundService(Intent(ctx, SwitcherService::class.java))
            } catch (_: Exception) {}
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, SwitcherService::class.java))
        }

        fun refresh() { instance?.apply() }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SwitcherService.startIfReady(context)
        }
    }
}
