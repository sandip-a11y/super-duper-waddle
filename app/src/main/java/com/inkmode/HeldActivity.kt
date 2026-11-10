package com.inkmode

import android.app.Activity
import android.app.ActivityOptions
import android.os.Bundle
import android.text.format.DateUtils

/** Everything that was held back, newest first. Tap to open, then it's removed from the list. */
class HeldActivity : Activity() {
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val col = Ui.scrollPage(this)
        val items = HeldStore.all(this).reversed()
        col.addView(Ui.text(this, "Held notifications", 28f, bold = true))
        if (items.isEmpty()) {
            col.addView(Ui.text(this, "Nothing held right now.", 16f, faint = true))
            return
        }
        col.addView(Ui.link(this, "clear all", 16f) { HeldStore.clear(this); render() })
        col.addView(Ui.divider(this))
        for (h in items) {
            val app = Apps.label(this, h.pkg) ?: h.pkg
            val ago = DateUtils.getRelativeTimeSpanString(h.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            col.addView(Ui.text(this, "$app · $ago", 13f, faint = true).apply { setPadding(0, 0, 0, 0) })
            col.addView(Ui.text(this, h.title.ifBlank { app }, 19f, bold = true).apply {
                setPadding(0, 0, 0, 0)
                isClickable = true
                setOnClickListener { open(h) }
            })
            if (h.text.isNotBlank()) {
                col.addView(Ui.text(this, h.text, 16f).apply {
                    maxLines = 4
                    isClickable = true
                    setOnClickListener { open(h) }
                })
            }
            col.addView(Ui.divider(this))
        }
    }

    @Suppress("DEPRECATION")
    private fun open(h: Held) {
        val pi = HeldStore.tapIntent(h.key)
        HeldStore.remove(this, h.key)
        val opened = pi != null && try {
            val opts = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            pi.send(this, 0, null, null, null, null, opts.toBundle())
            true
        } catch (_: Exception) { false }
        if (!opened) Apps.launch(this, h.pkg)
    }
}
