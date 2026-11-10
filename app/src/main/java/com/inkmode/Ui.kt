package com.inkmode

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Small helpers so every screen shares the same flat, serif, e-paper style. */
object Ui {
    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun ink(ctx: Context) = ctx.getColor(R.color.ink)
    fun faint(ctx: Context) = ctx.getColor(R.color.faint)
    fun paper(ctx: Context) = ctx.getColor(R.color.paper)

    fun text(ctx: Context, s: CharSequence, sizeSp: Float = 18f, bold: Boolean = false, faint: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = sizeSp
            setTextColor(if (faint) faint(ctx) else ink(ctx))
            typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            val pad = dp(ctx, 8)
            setPadding(0, pad, 0, pad)
        }

    fun link(ctx: Context, s: CharSequence, sizeSp: Float = 18f, onClick: () -> Unit): TextView =
        text(ctx, s, sizeSp).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            isClickable = true
            setOnClickListener { onClick() }
        }

    fun divider(ctx: Context): View = View(ctx).apply {
        setBackgroundColor(faint(ctx))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1)).apply {
            topMargin = dp(ctx, 12)
            bottomMargin = dp(ctx, 12)
        }
    }

    fun column(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
    }

    /** Keeps content clear of the status and navigation bars (apps are edge-to-edge on Android 15+). */
    fun padForBars(view: View, basePx: Int) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(basePx + b.left, basePx + b.top, basePx + b.right, basePx + b.bottom)
            insets
        }
    }

    /** A scrolling page with no overscroll glow. Returns the column to fill. */
    fun scrollPage(activity: Activity): LinearLayout {
        val col = column(activity)
        val scroll = ScrollView(activity).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            setBackgroundColor(paper(activity))
            addView(col)
        }
        padForBars(scroll, dp(activity, 24))
        activity.setContentView(scroll)
        return col
    }
}
