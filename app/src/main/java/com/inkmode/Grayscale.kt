package com.inkmode

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.util.Log

/**
 * Turns the phone's grayscale on and off without ADB.
 *
 * Android 15 lets an app own a "Mode" whose device effects include grayscale. The Mode uses
 * INTERRUPTION_FILTER_ALL, so it changes only the display and never silences anything.
 * It needs Do Not Disturb access, which the user grants with a toggle in Settings.
 */
object Grayscale {
    private const val TAG = "Inkmode"
    private val CONDITION: Uri = Uri.parse("condition://com.inkmode/grayscale")

    @Volatile private var lastState: Boolean? = null

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun hasAccess(ctx: Context): Boolean = nm(ctx).isNotificationPolicyAccessGranted

    private fun ensureRule(ctx: Context): String? {
        if (!hasAccess(ctx)) return null
        val prefs = Prefs(ctx)
        val nm = nm(ctx)
        prefs.ruleId?.let { id ->
            val existing = try { nm.getAutomaticZenRule(id) } catch (e: Exception) { null }
            if (existing != null) return id
        }
        val rule = AutomaticZenRule.Builder(ctx.getString(R.string.mode_name), CONDITION)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setConfigurationActivity(ComponentName(ctx, MainActivity::class.java))
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            .setDeviceEffects(ZenDeviceEffects.Builder().setShouldDisplayGrayscale(true).build())
            .setTriggerDescription("Inkmode turns this on outside your colour apps")
            .setManualInvocationAllowed(true)
            .setEnabled(true)
            .build()
        return try {
            nm.addAutomaticZenRule(rule).also { prefs.ruleId = it }
        } catch (e: Exception) {
            Log.w(TAG, "Could not create grayscale mode", e)
            null
        }
    }

    /** Returns false if the change could not be made (no access, or the system refused). */
    fun set(ctx: Context, on: Boolean, force: Boolean = false): Boolean {
        if (!force && lastState == on) return true
        val id = ensureRule(ctx) ?: return false
        return try {
            val state = if (on) Condition.STATE_TRUE else Condition.STATE_FALSE
            nm(ctx).setAutomaticZenRuleState(
                id, Condition(CONDITION, if (on) "On" else "Off", state, Condition.SOURCE_CONTEXT)
            )
            lastState = on
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not switch grayscale", e)
            false
        }
    }

    /** Deletes the Mode entirely (used by "Undo everything"). */
    fun remove(ctx: Context) {
        val prefs = Prefs(ctx)
        val id = prefs.ruleId ?: return
        try { nm(ctx).removeAutomaticZenRule(id) } catch (_: Exception) {}
        prefs.ruleId = null
        lastState = null
    }
}
