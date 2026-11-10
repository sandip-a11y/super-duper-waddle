package com.inkmode

import android.app.PendingIntent
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class Held(val key: String, val pkg: String, val title: String, val text: String, val time: Long)

/** Notifications held back for the next digest. Text is saved; tap targets live only in memory. */
object HeldStore {
    private const val FILE = "held"
    private const val MAX = 200
    private val intents = ConcurrentHashMap<String, PendingIntent>()

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @Synchronized
    fun all(ctx: Context): List<Held> {
        val arr = try { JSONArray(sp(ctx).getString("items", "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Held(o.getString("k"), o.getString("p"), o.optString("t"), o.optString("x"), o.getLong("w"))
        }
    }

    @Synchronized
    private fun save(ctx: Context, items: List<Held>) {
        val arr = JSONArray()
        items.takeLast(MAX).forEach {
            arr.put(JSONObject().put("k", it.key).put("p", it.pkg).put("t", it.title).put("x", it.text).put("w", it.time))
        }
        sp(ctx).edit().putString("items", arr.toString()).apply()
    }

    @Synchronized
    fun add(ctx: Context, item: Held, tap: PendingIntent?) {
        val items = all(ctx).filterNot { it.key == item.key } + item
        save(ctx, items)
        if (tap != null) intents[item.key] = tap else intents.remove(item.key)
        sp(ctx).edit().putInt("new", newSinceDigest(ctx) + 1).apply()
    }

    @Synchronized
    fun remove(ctx: Context, key: String) {
        save(ctx, all(ctx).filterNot { it.key == key })
        intents.remove(key)
    }

    @Synchronized
    fun clear(ctx: Context) {
        save(ctx, emptyList())
        intents.clear()
        sp(ctx).edit().putInt("new", 0).apply()
    }

    fun count(ctx: Context) = all(ctx).size
    fun tapIntent(key: String): PendingIntent? = intents[key]
    fun newSinceDigest(ctx: Context) = sp(ctx).getInt("new", 0)
    fun markDigested(ctx: Context) = sp(ctx).edit().putInt("new", 0).apply()
}
