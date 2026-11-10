package com.inkmode

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** App list that turns pages instead of scrolling, like an e-reader. */
class AllAppsActivity : Activity() {
    private var apps: List<AppEntry> = emptyList()
    private var page = 0
    private lateinit var list: LinearLayout
    private lateinit var pager: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.column(this).apply { setBackgroundColor(Ui.paper(this@AllAppsActivity)) }
        Ui.padForBars(root, Ui.dp(this, 24))
        root.addView(Ui.text(this, "All apps", 28f, bold = true))
        root.addView(Ui.text(this, "Tap to open · long-press to add to or remove from home", 14f, faint = true))
        root.addView(Ui.divider(this))
        list = Ui.column(this)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val w = { LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        nav.addView(Ui.link(this, "‹ previous", 16f) { turn(-1) }, w())
        pager = Ui.text(this, "", 16f, faint = true).apply { gravity = Gravity.CENTER_HORIZONTAL }
        nav.addView(pager, w())
        nav.addView(Ui.link(this, "next ›", 16f) { turn(1) }.apply { gravity = Gravity.END }, w())
        root.addView(nav)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        apps = Apps.all(this)
        show()
    }

    private val perPage: Int
        get() {
            // Roughly 52dp per row; fit as many as the screen allows.
            val h = list.height
            return if (h > 0) maxOf(4, h / Ui.dp(this, 52)) else 10
        }

    private fun pages() = maxOf(1, (apps.size + perPage - 1) / perPage)

    private fun turn(delta: Int) {
        page = (page + delta).coerceIn(0, pages() - 1)
        show()
    }

    private fun show() {
        list.post { render() }
    }

    private fun render() {
        list.removeAllViews()
        page = page.coerceIn(0, pages() - 1)
        val pinned = Prefs(this).pinned ?: emptyList()
        apps.drop(page * perPage).take(perPage).forEach { app ->
            val mark = if (app.pkg in pinned) "  •" else ""
            list.addView(Ui.text(this, app.label + mark, 22f).apply {
                isClickable = true
                setOnClickListener { Apps.launch(this@AllAppsActivity, app) }
                setOnLongClickListener {
                    val now = Apps.togglePin(this@AllAppsActivity, app.pkg)
                    Toast.makeText(
                        this@AllAppsActivity,
                        if (now) "Added ${app.label} to home" else "Removed ${app.label} from home",
                        Toast.LENGTH_SHORT
                    ).show()
                    render()
                    true
                }
            })
        }
        pager.text = "${page + 1} / ${pages()}"
        list.visibility = View.VISIBLE
    }
}
