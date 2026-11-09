package com.moneytrack.app

import android.content.Context
import org.json.JSONArray
import java.security.MessageDigest

object Store {
    private const val PREF = "moneytrack_local"
    private const val KEY = "transactions"

    @Synchronized fun all(context: Context): MutableList<Tx> {
        val raw = context.getSharedPreferences(PREF, 0).getString(KEY, "[]") ?: "[]"
        val a = JSONArray(raw)
        val out = mutableListOf<Tx>()
        for (i in 0 until a.length()) runCatching { out.add(Tx.fromJson(a.getJSONObject(i))) }
        return out.sortedByDescending { it.timestamp }.toMutableList()
    }

    @Synchronized fun add(context: Context, tx: Tx): Boolean {
        val list = all(context)
        if (list.any { it.id == tx.id }) return false
        list.add(tx)
        val a = JSONArray()
        list.take(5000).forEach { a.put(it.toJson()) }
        context.getSharedPreferences(PREF, 0).edit().putString(KEY, a.toString()).apply()
        return true
    }

    fun fingerprint(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, 0).edit().remove(KEY).apply()
    }
}
