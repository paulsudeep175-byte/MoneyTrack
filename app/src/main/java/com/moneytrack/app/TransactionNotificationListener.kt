package com.moneytrack.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Locale
import java.util.UUID

class TransactionNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName.lowercase(Locale.ROOT)
        val appName = when {
            pkg.contains("google") && (pkg.contains("pay") || pkg.contains("android.apps")) -> "Google Pay"
            pkg.contains("phonepe") -> "PhonePe"
            pkg.contains("paytm") -> "Paytm"
            pkg.contains("bhim") -> "BHIM"
            pkg.contains("amazon") -> "Amazon Pay"
            pkg.contains("sbi") || pkg.contains("hdfc") || pkg.contains("icici") ||
                pkg.contains("axis") || pkg.contains("kotak") || pkg.contains("bank") -> "Bank notification"
            else -> return // Ignore unrelated app notifications
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }.orEmpty()
        val combined = listOf(title, text, lines).filter { it.isNotBlank() }.distinct().joinToString(" | ")
        if (combined.isBlank()) return

        val amountRegex = Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:inr|rs\.?)""")
        val match = amountRegex.find(combined) ?: return
        val amountText = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return
        val amount = amountText.replace(",", "").toDoubleOrNull() ?: return
        if (amount <= 0.0) return

        val lower = combined.lowercase(Locale.ROOT)
        val incoming = listOf("received", "credited", "credit", "from ").any { lower.contains(it) }
        val outgoing = listOf("paid", "sent", "debited", "debit", "to ").any { lower.contains(it) }
        if (!incoming && !outgoing) return // Avoid guessing direction from ambiguous alerts
        val direction = if (incoming && !outgoing) "IN" else if (outgoing && !incoming) "OUT" else {
            // When both words appear, prefer the explicit transaction verb closest to the amount.
            if (Regex("""(?i)(received|credited)""").containsMatchIn(lower)) "IN" else "OUT"
        }

        val person = extractPerson(combined, direction)
        val stable = listOf(sbn.packageName, sbn.postTime.toString(), combined).joinToString("|")
        val tx = Tx(
            id = Store.fingerprint(stable),
            timestamp = sbn.postTime,
            direction = direction,
            amount = amount,
            person = person,
            app = appName,
            raw = combined
        )
        Store.add(this, tx)
    }

    private fun extractPerson(text: String, direction: String): String {
        val patterns = if (direction == "IN") listOf(
            Regex("""(?i)(?:from|by)\s+(.+?)(?:\s+(?:via|on|at|\.|UPI|using)\b|[|.]|$)""")
        ) else listOf(
            Regex("""(?i)(?:to|paid to|sent to)\s+(.+?)(?:\s+(?:via|on|at|\.|UPI|using)\b|[|.]|$)""")
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val candidate = m.groupValues[1].trim().trim('-', ':', ' ')
                if (candidate.length in 2..60) return candidate
            }
        }
        val upi = Regex("""[A-Za-z0-9._-]{2,}@[A-Za-z0-9.-]{2,}""").find(text)?.value
        return upi ?: "Unknown person"
    }
}
