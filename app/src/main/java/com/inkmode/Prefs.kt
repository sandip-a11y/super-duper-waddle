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

    companion object {
        val DEFAULT_COLOUR_APPS = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.whatsapp",
            "com.google.android.gm",
        )
    }
}
