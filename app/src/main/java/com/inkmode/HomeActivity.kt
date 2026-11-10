package com.inkmode

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

/** Minimal text-only home screen: clock, date, battery, a short app list. */
class HomeActivity : Activity() {
    private lateinit var battery: TextView
    private lateinit var appList: LinearLayout

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = showBattery(i)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Home has nowhere to go back to.
        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {}

        val root = Ui.column(this).apply { setBackgroundColor(Ui.paper(this@HomeActivity)) }
        Ui.padForBars(root, Ui.dp(this, 28))

        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        root.addView(TextClock(this).apply {
            format12Hour = "h:mm"
            format24Hour = "HH:mm"
            textSize = 76f
            typeface = serif
            setTextColor(Ui.ink(this@HomeActivity))
        })
        root.addView(TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"
            format24Hour = "EEEE, d MMMM"
            textSize = 20f
            typeface = serif
            setTextColor(Ui.ink(this@HomeActivity))
        })
        battery = Ui.text(this, "", 16f, faint = true)
        root.addView(battery)

        root.addView(View(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        appList = Ui.column(this)
        root.addView(appList)
        root.addView(View(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val weight = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        bottom.addView(Ui.link(this, "all apps", 16f) { open(AllAppsActivity::class.java) }, weight)
        bottom.addView(Ui.link(this, "settings", 16f) { open(MainActivity::class.java) }.apply { gravity = Gravity.END }, weight)
        root.addView(bottom)

        setContentView(root)
    }

    private fun open(cls: Class<*>) {
        startActivity(Intent(this, cls), android.app.ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            ?.let { showBattery(it) }
        showApps()
        SwitcherService.startIfReady(this)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
    }

    private fun showBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0) return
        val pct = level * 100 / scale
        val charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        battery.text = if (charging) "$pct% · charging" else "$pct%"
    }

    private fun showApps() {
        val prefs = Prefs(this)
        val pinned = prefs.pinned ?: Apps.defaultPinned(this).also { prefs.pinned = it }
        appList.removeAllViews()
        for (pkg in pinned) {
            val label = Apps.label(this, pkg) ?: continue
            val row = Ui.text(this, label, 26f).apply {
                isClickable = true
                setOnClickListener { Apps.launch(this@HomeActivity, pkg) }
                setOnLongClickListener {
                    Apps.togglePin(this@HomeActivity, pkg)
                    Toast.makeText(this@HomeActivity, "Removed $label from home", Toast.LENGTH_SHORT).show()
                    showApps()
                    true
                }
            }
            appList.addView(row)
        }
        if (appList.childCount == 0) {
            appList.addView(Ui.text(this, "Long-press an app in \"all apps\" to put it here.", 16f, faint = true))
        }
    }
}
