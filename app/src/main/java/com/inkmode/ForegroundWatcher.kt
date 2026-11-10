package com.inkmode

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Watches which app is in front and switches grayscale off for colour apps.
 * Only reacts to real activity changes, so keyboards, pop-ups and overlays don't flip the state.
 */
class ForegroundWatcher : AccessibilityService() {
    private var ignored: Set<String> = emptySet()
    private var current: String? = null
    private val activityCache = HashMap<String, Boolean>()

    override fun onServiceConnected() {
        instance = this
        val imes = try {
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
        } catch (_: Exception) { emptyList() }
        ignored = setOf("com.android.systemui", "android") + imes
        apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored) return
        val cls = event.className?.toString() ?: return
        if (!isActivity(pkg, cls)) return
        if (pkg == current) return
        current = pkg
        apply()
    }

    private fun isActivity(pkg: String, cls: String): Boolean {
        val key = "$pkg/$cls"
        return activityCache.getOrPut(key) {
            try {
                packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
                true
            } catch (_: Exception) { false }
        }
    }

    fun apply() {
        val prefs = Prefs(this)
        val pkg = current
        val gray = prefs.enabled && (pkg == null || pkg !in prefs.colourApps)
        Grayscale.set(this, gray)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        // If the switcher stops, don't leave the phone stuck in grayscale inside a colour app.
        Grayscale.set(this, false, force = true)
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile var instance: ForegroundWatcher? = null
        fun refresh() { instance?.apply() }
    }
}
