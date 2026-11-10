package com.inkmode

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import android.widget.Toast

data class AppEntry(val label: String, val pkg: String, val component: ComponentName)

object Apps {
    fun all(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map {
                val ai = it.activityInfo
                AppEntry(it.loadLabel(pm).toString(), ai.packageName, ComponentName(ai.packageName, ai.name))
            }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
    }

    fun label(ctx: Context, pkg: String): String? = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))).toString()
    } catch (_: Exception) { null }

    fun isLaunchable(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    /** First-run home screen: phone, messages, camera, then the user's four apps, if installed. */
    fun defaultPinned(ctx: Context): List<String> {
        val pm = ctx.packageManager
        val dialer = try { ctx.getSystemService(TelecomManager::class.java).defaultDialerPackage } catch (_: Exception) { null }
        val sms = try { Telephony.Sms.getDefaultSmsPackage(ctx) } catch (_: Exception) { null }
        val camera = pm.resolveActivity(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )?.activityInfo?.packageName?.takeIf { it != "android" }
        return listOfNotNull(
            dialer, sms, camera,
            "com.whatsapp", "com.google.android.gm", "com.google.android.youtube", "com.instagram.android",
        ).distinct().filter { isLaunchable(ctx, it) }
    }

    private fun noAnimation(activity: Activity) =
        ActivityOptions.makeCustomAnimation(activity, 0, 0).toBundle()

    fun launch(activity: Activity, entry: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        start(activity, i)
    }

    fun launch(activity: Activity, pkg: String) {
        val i = activity.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) {
            Toast.makeText(activity, "App not installed", Toast.LENGTH_SHORT).show()
            return
        }
        start(activity, i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun start(activity: Activity, i: Intent) {
        try {
            activity.startActivity(i, noAnimation(activity))
        } catch (_: Exception) {
            Toast.makeText(activity, "Could not open app", Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePin(ctx: Context, pkg: String): Boolean {
        val prefs = Prefs(ctx)
        val list = (prefs.pinned ?: defaultPinned(ctx)).toMutableList()
        val nowPinned = if (pkg in list) { list.remove(pkg); false } else { list.add(pkg); true }
        prefs.pinned = list
        return nowPinned
    }
}
