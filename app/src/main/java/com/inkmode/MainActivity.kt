package com.inkmode

import android.Manifest
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
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast

/** Setup checklist plus all settings. Also opened from the Mode's entry in system Settings. */
class MainActivity : Activity() {
    private lateinit var col: LinearLayout

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        col = Ui.scrollPage(this)
        val prefs = Prefs(this)

        col.addView(Ui.text(this, "Inkmode", 32f, bold = true))
        col.addView(Ui.text(this, "E-paper look for your phone. Do the setup steps in order.", 16f, faint = true))

        section("Setup")
        step("1. Allow restricted settings", null,
            "Needed before steps 2 and 4 can be turned on. On the next screen tap ⋮ (top right) → Allow restricted settings. " +
                "If you don't see ⋮, try step 2 first: Android shows the option after it blocks you once.") {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        step("2. Turn on the colour switch", watcherOn(),
            "Settings → Accessibility → Inkmode colour switch → on.") {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        step("3. Allow Do Not Disturb access", Grayscale.hasAccess(this),
            "Lets Inkmode switch grayscale. It never silences anything.") {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        step("4. Allow notification access", listenerOn(),
            "Only needed for notification batching.") {
            open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(this, NotificationBatcher::class.java).flattenToString()),
                fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        step("5. Allow Inkmode's own notifications", notifPermission(),
            "So the batched digest can be shown.") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        step("6. Make Inkmode your home screen", isHome(), null) {
            val rm = getSystemService(RoleManager::class.java)
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 2)
        }

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
            if (!prefs.enabled) Grayscale.set(this, false, force = true)
            ForegroundWatcher.refresh()
        }
        val colourNames = prefs.colourApps.mapNotNull { Apps.label(this, it) }.sorted()
        col.addView(Ui.link(this, "Colour apps (${colourNames.size})") { picker(AppPickerActivity.MODE_COLOUR) })
        col.addView(Ui.text(this, colourNames.joinToString(", ").ifBlank { "None" }, 14f, faint = true))
        col.addView(Ui.link(this, "Test grayscale for 5 seconds") { testGrayscale() })

        section("Notification batching")
        toggle("Hold notifications and deliver them in batches", prefs.batching) {
            prefs.batching = !prefs.batching
            Digest.schedule(this)
        }
        col.addView(Ui.link(this, "Deliver every ${prefs.batchMinutes} minutes (tap to change)") {
            val c = Prefs.BATCH_CHOICES
            prefs.batchMinutes = c[(c.indexOf(prefs.batchMinutes) + 1) % c.size]
            Digest.schedule(this)
            render()
        })
        col.addView(Ui.link(this, "Always let through (${prefs.letThrough.count { Apps.isLaunchable(this, it) }} apps)") {
            picker(AppPickerActivity.MODE_LET_THROUGH)
        })
        col.addView(Ui.link(this, "Held notifications (${HeldStore.count(this)})") { open(Intent(this, HeldActivity::class.java)) })

        section("Undo")
        col.addView(Ui.link(this, "Turn everything off") { undoAll() })
        col.addView(Ui.text(this,
            "Stops grayscale, removes Inkmode's Mode and stops batching. To get your old home screen back, " +
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

    private fun picker(mode: String) =
        open(Intent(this, AppPickerActivity::class.java).putExtra(AppPickerActivity.EXTRA_MODE, mode))

    private fun open(i: Intent, fallback: Intent? = null) {
        try { startActivity(i) } catch (_: Exception) {
            try { if (fallback != null) startActivity(fallback) } catch (_: Exception) {
                Toast.makeText(this, "Couldn't open that screen on this phone", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun watcherOn(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val me = ComponentName(this, ForegroundWatcher::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun listenerOn(): Boolean =
        getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(this, NotificationBatcher::class.java))

    private fun notifPermission() =
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun isHome() = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    private fun testGrayscale() {
        if (!Grayscale.hasAccess(this)) {
            Toast.makeText(this, "Do step 3 first", Toast.LENGTH_LONG).show()
            return
        }
        if (!Grayscale.set(this, true, force = true)) {
            Toast.makeText(this, "This phone refused the grayscale Mode. Please tell me (the developer).", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Grayscale on for 5 seconds", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            // Back to whatever the switch would choose (this screen itself is grayscale when set up).
            val keepOn = ForegroundWatcher.instance != null && Prefs(this).enabled
            Grayscale.set(this, keepOn, force = true)
        }, 5000)
    }

    private fun undoAll() {
        val prefs = Prefs(this)
        prefs.enabled = false
        prefs.batching = false
        Grayscale.set(this, false, force = true)
        Grayscale.remove(this)
        Digest.schedule(this)
        Toast.makeText(this, "Inkmode is off. You can also turn off its accessibility service.", Toast.LENGTH_LONG).show()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        render()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }
}
