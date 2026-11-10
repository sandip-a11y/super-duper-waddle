package com.inkmode

import android.app.Activity
import android.os.Bundle
import android.widget.CheckBox

/** Checklist of apps, used for both "colour apps" and "always let through". */
class AppPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_COLOUR
        val prefs = Prefs(this)
        val col = Ui.scrollPage(this)

        val title = if (mode == MODE_COLOUR) "Colour apps" else "Always let through"
        val hint = if (mode == MODE_COLOUR)
            "These apps stay in full colour. Everything else is grayscale."
        else
            "Notifications from these apps are never held back. Calls and alarms always come through."
        col.addView(Ui.text(this, title, 28f, bold = true))
        col.addView(Ui.text(this, hint, 15f, faint = true))
        col.addView(Ui.divider(this))

        val selected = (if (mode == MODE_COLOUR) prefs.colourApps else prefs.letThrough).toMutableSet()
        Apps.all(this).distinctBy { it.pkg }.forEach { app ->
            col.addView(CheckBox(this).apply {
                text = app.label
                textSize = 18f
                setTextColor(Ui.ink(this@AppPickerActivity))
                buttonTintList = android.content.res.ColorStateList.valueOf(Ui.ink(this@AppPickerActivity))
                isChecked = app.pkg in selected
                setPadding(Ui.dp(this@AppPickerActivity, 8), 0, 0, 0)
                minHeight = Ui.dp(this@AppPickerActivity, 48)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(app.pkg) else selected.remove(app.pkg)
                    if (mode == MODE_COLOUR) {
                        prefs.colourApps = selected.toSet()
                        ForegroundWatcher.refresh()
                    } else {
                        prefs.letThrough = selected.toSet()
                    }
                }
            })
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_COLOUR = "colour"
        const val MODE_LET_THROUGH = "let_through"
    }
}
