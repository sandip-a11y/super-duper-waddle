package com.inkmode

import android.app.Activity
import android.app.NotificationManager
import android.app.UiModeManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast

/** Setup checklist plus all settings. Also opened from the Mode's entry in system Settings. */
class MainActivity : Activity() {
    private lateinit var col: LinearLayout

    override fun onResume() {
        super.onResume()
        SwitcherService.startIfReady(this)
        render()
    }

    private fun render() {
        col = Ui.scrollPage(this)
        val prefs = Prefs(this)

        col.addView(Ui.text(this, "Inkmode", 32f, bold = true))
        col.addView(Ui.text(this, "E-paper look for your phone. Do the setup steps in order.", 16f, faint = true))

        section("Setup")
        step("1. Allow Do Not Disturb access", Grayscale.hasAccess(this),
            "Lets Inkmode switch grayscale. It never silences anything.") {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        step("2. Allow usage access", SwitcherService.hasUsageAccess(this),
            "Lets Inkmode see which app is open. If the switch is greyed out: App info → ⋮ (top right) → Allow restricted settings, then try again.") {
            open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")),
                fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        step("3. Let Inkmode run in the background", ignoresBatteryLimits(),
            "Choose Allow, or Battery → Unrestricted. Without it the phone may stop the colour switch.") {
            @Suppress("BatteryLife")
            open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
                fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        step("4. Make Inkmode your home screen", isHome(), null) {
            val rm = getSystemService(RoleManager::class.java)
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 2)
        }
        val running = SwitcherService.instance != null
        col.addView(Ui.text(this,
            if (running) "Colour switch: running" else "Colour switch: starts when steps 1–2 are done",
            15f, faint = !running))

        section("Phone settings (one-time, you change these)")
        val animOff = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        step("Remove animations", animOff, "Settings → Accessibility → Remove animations → on.") {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        val hz = display?.refreshRate?.let { Math.round(it) } ?: 0
        step("Refresh rate: 60 Hz", if (hz in 1..61) true else null,
            "Settings → Display → Refresh rate → 60 Hz. Right now: $hz Hz.") {
            open(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        val night = getSystemService(UiModeManager::class.java).nightMode
        val scheduled = night == UiModeManager.MODE_NIGHT_AUTO || night == UiModeManager.MODE_NIGHT_CUSTOM
        step("Dark theme on a schedule", scheduled,
            "Turn on Dark theme → Schedule → Sunset to sunrise. Inkmode switches between paper (day) and black ink (night) with it.") {
            open(Intent("android.settings.DARK_THEME_SETTINGS"), fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        step("Night Light on a schedule", null,
            "Warm tint for the evening. Display → Night Light → Schedule → Sunset to sunrise.") {
            open(Intent("android.settings.NIGHT_DISPLAY_SETTINGS"), fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        step("Extra dim shortcut", null,
            "Accessibility → Extra dim → turn on the shortcut. Dimmer than minimum brightness, with less flicker at night.") {
            open(Intent("android.settings.REDUCE_BRIGHT_COLORS_SETTINGS"), fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        section("E-paper look")
        toggle("Grayscale outside colour apps", prefs.enabled) {
            prefs.enabled = !prefs.enabled
            if (prefs.enabled) SwitcherService.startIfReady(this) else SwitcherService.stop(this)
            if (!prefs.enabled) Grayscale.set(this, false, force = true)
        }
        val colourNames = prefs.colourApps.mapNotNull { Apps.label(this, it) }.sorted()
        col.addView(Ui.link(this, "Colour apps (${colourNames.size})") {
            open(Intent(this, AppPickerActivity::class.java))
        })
        col.addView(Ui.text(this, colourNames.joinToString(", ").ifBlank { "None" }, 14f, faint = true))
        col.addView(Ui.link(this, "Test grayscale for 5 seconds") { testGrayscale() })

        section("Undo")
        col.addView(Ui.link(this, "Turn everything off") { undoAll() })
        col.addView(Ui.text(this,
            "Stops grayscale and removes Inkmode's Mode. To get your old home screen back, " +
                "pick it in Settings → Apps → Default apps → Home app.", 14f, faint = true))
    }

    private fun section(title: String) {
        col.addView(Ui.divider(this))
        col.addView(Ui.text(this, title, 22f, bold = true))
    }

    /** done: true = done, false = still needed, null = can't be checked. */
    private fun step(title: String, done: Boolean?, hint: String?, action: () -> Unit) {
        val mark = when (done) { true -> "✓ "; false -> "○ "; null -> "· " }
        col.addView(Ui.link(this, mark + title, 18f) { action() })
        if (hint != null) col.addView(Ui.text(this, hint, 14f, faint = true).apply { setPadding(0, 0, 0, Ui.dp(this@MainActivity, 8)) })
    }

    private fun toggle(title: String, on: Boolean, flip: () -> Unit) {
        col.addView(Ui.link(this, "${if (on) "On" else "Off"} · $title") { flip(); render() })
    }

    private fun open(i: Intent, fallback: Intent? = null) {
        try { startActivity(i) } catch (_: Exception) {
            try { if (fallback != null) startActivity(fallback) } catch (_: Exception) {
                Toast.makeText(this, "Couldn't open that screen on this phone", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ignoresBatteryLimits() =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun isHome() = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    private fun testGrayscale() {
        if (!Grayscale.hasAccess(this)) {
            Toast.makeText(this, "Do step 1 first", Toast.LENGTH_LONG).show()
            return
        }
        if (!Grayscale.set(this, true, force = true)) {
            Toast.makeText(this, "This phone refused the grayscale Mode. Please tell me (the developer).", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Grayscale on for 5 seconds", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            // Back to whatever the switch would choose (this screen itself is grayscale when set up).
            val keepOn = SwitcherService.instance != null && Prefs(this).enabled
            Grayscale.set(this, keepOn, force = true)
        }, 5000)
    }

    private fun undoAll() {
        val prefs = Prefs(this)
        prefs.enabled = false
        SwitcherService.stop(this)
        Grayscale.set(this, false, force = true)
        Grayscale.remove(this)
        Toast.makeText(this, "Inkmode is off.", Toast.LENGTH_LONG).show()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        render()
    }
}
