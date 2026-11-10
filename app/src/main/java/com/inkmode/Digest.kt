package com.inkmode

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

object Digest {
    private const val CHANNEL = "digest"
    private const val ID = 1

    private fun alarmIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, DigestReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Inexact, non-waking alarm: the digest waits until the phone is awake anyway. */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx)
        am.cancel(pi)
        val prefs = Prefs(ctx)
        if (!prefs.batching) return
        val every = prefs.batchMinutes * 60_000L
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + every, every, pi)
    }

    fun post(ctx: Context) {
        val fresh = HeldStore.newSinceDigest(ctx)
        if (fresh == 0) return
        val items = HeldStore.all(ctx)
        if (items.isEmpty()) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Notification digest", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val apps = items.map { Apps.label(ctx, it.pkg) ?: it.pkg }.distinct()
        val style = Notification.InboxStyle()
        items.takeLast(6).reversed().forEach { style.addLine("${Apps.label(ctx, it.pkg) ?: it.pkg}: ${it.title}") }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, HeldActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("${items.size} held notifications")
            .setContentText(apps.joinToString(", "))
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { nm.notify(ID, n) } catch (_: SecurityException) {}
        HeldStore.markDigested(ctx)
    }
}

class DigestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Digest.post(context)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Digest.schedule(context)
    }
}
