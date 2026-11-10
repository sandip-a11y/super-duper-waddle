package com.inkmode

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.CheckBox

/** Checklist of apps that stay in full colour. */
class AppPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val col = Ui.scrollPage(this)
        col.addView(Ui.text(this, "Colour apps", 28f, bold = true))
        col.addView(Ui.text(this, "These apps stay in full colour. Everything else is grayscale.", 15f, faint = true))
        col.addView(Ui.divider(this))

        val selected = prefs.colourApps.toMutableSet()
        Apps.all(this).distinctBy { it.pkg }.forEach { app ->
            col.addView(CheckBox(this).apply {
                text = app.label
                textSize = 18f
                setTextColor(Ui.ink(this@AppPickerActivity))
                buttonTintList = ColorStateList.valueOf(Ui.ink(this@AppPickerActivity))
                isChecked = app.pkg in selected
                setPadding(Ui.dp(this@AppPickerActivity, 8), 0, 0, 0)
                minHeight = Ui.dp(this@AppPickerActivity, 48)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(app.pkg) else selected.remove(app.pkg)
                    prefs.colourApps = selected.toSet()
                    SwitcherService.refresh()
                }
            })
        }
    }
}
