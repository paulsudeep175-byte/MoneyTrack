package com.moneytrack.app

import org.json.JSONObject

data class Tx(
    val id: String,
    val timestamp: Long,
    val direction: String, // IN or OUT
    val amount: Double,
    val person: String,
    val app: String,
    val raw: String
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("timestamp", timestamp); put("direction", direction)
        put("amount", amount); put("person", person); put("app", app); put("raw", raw)
    }
    companion object {
        fun fromJson(j: JSONObject) = Tx(
            j.optString("id"), j.optLong("timestamp"), j.optString("direction", "OUT"),
            j.optDouble("amount"), j.optString("person", "Unknown"),
            j.optString("app", "Other"), j.optString("raw")
        )
    }
}
