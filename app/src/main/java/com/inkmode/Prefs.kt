package com.inkmode

import android.content.Context
import android.content.SharedPreferences

/** All user settings in one place. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("inkmode", Context.MODE_PRIVATE)

    /** Master switch for the grayscale look. */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** Apps that stay in full colour. */
    var colourApps: Set<String>
        get() = sp.getStringSet("colour_apps", null)?.toSet() ?: DEFAULT_COLOUR_APPS
        set(v) = sp.edit().putStringSet("colour_apps", v).apply()

    /** Home screen apps, in order. Null until the user (or first run) sets them. */
    var pinned: List<String>?
        get() = sp.getString("pinned", null)?.split('\n')?.filter { it.isNotBlank() }
        set(v) = sp.edit().putString("pinned", v?.joinToString("\n")).apply()

    var ruleId: String?
        get() = sp.getString("rule_id", null)
        set(v) = sp.edit().putString("rule_id", v).apply()

    /** Off by default so a fresh install never hides notifications without the user asking. */
    var batching: Boolean
        get() = sp.getBoolean("batching", false)
        set(v) = sp.edit().putBoolean("batching", v).apply()

    var batchMinutes: Int
        get() = sp.getInt("batch_minutes", 60)
        set(v) = sp.edit().putInt("batch_minutes", v).apply()

    /** Apps whose notifications are never held back. */
    var letThrough: Set<String>
        get() = sp.getStringSet("let_through", null)?.toSet() ?: DEFAULT_LET_THROUGH
        set(v) = sp.edit().putStringSet("let_through", v).apply()

    companion object {
        val DEFAULT_COLOUR_APPS = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.whatsapp",
            "com.google.android.gm",
        )
        val DEFAULT_LET_THROUGH = setOf(
            "com.whatsapp",
            // SMS app, so one-time passwords arrive straight away.
            "com.google.android.apps.messaging",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.systemui",
            "android",
        )
        val BATCH_CHOICES = listOf(30, 60, 120)
    }
}
