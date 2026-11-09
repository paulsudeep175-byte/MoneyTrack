package com.moneytrack.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var search: EditText
    private lateinit var listBox: LinearLayout
    private var showPeople = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() { super.onResume(); if (::root.isInitialized) render() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = 14f, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size
            setTextColor(0xFF182235.toInt())
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; setOnClickListener { action() }
    }
    private fun money(n: Double) = "₹" + String.format(Locale("en", "IN"), "%,.2f", n)
    private fun allTx() = Store.all(this)

    private fun render() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFF4F6FB.toInt()) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(14))
            setBackgroundColor(-1)
        }
        header.addView(label("₹  MoneyTrack", 23f, true))
        header.addView(label("Automatic transaction and person-wise tracker", 12f))
        root.addView(header)
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(20)) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val access = NotificationAccess.isEnabled(this)
        val accessBox = card()
        accessBox.addView(label(if (access) "● Notification access is enabled" else "Notification access is not enabled", 15f, true))
        accessBox.addView(label("MoneyTrack can only detect transactions when supported apps show readable notifications. It cannot read old transaction history.", 12f))
        accessBox.addView(button(if (access) "Open notification access settings" else "Enable notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        content.addView(accessBox)

        val data = allTx()
        val incoming = data.filter { it.direction == "IN" }.sumOf { it.amount }
        val outgoing = data.filter { it.direction == "OUT" }.sumOf { it.amount }
        val stats = card()
        stats.addView(label("All-time overview", 17f, true))
        stats.addView(label("Transactions: ${data.size}    People: ${data.map { it.person }.distinct().size}", 13f))
        stats.addView(label("Money received: ${money(incoming)}", 16f, true).apply { setTextColor(0xFF12845F.toInt()) })
        stats.addView(label("Money sent: ${money(outgoing)}", 16f, true).apply { setTextColor(0xFFC34350.toInt()) })
        stats.addView(label("Net flow: ${money(incoming - outgoing)}", 16f, true))
        content.addView(stats)

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(button("Transactions") { showPeople = false; render() }, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(button("People totals") { showPeople = true; render() }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(tabs)

        search = EditText(this).apply {
            hint = if (showPeople) "Search person or UPI ID" else "Search transactions"
           setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        content.addView(search)
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listBox)
        search.addTextChangedListener(SimpleWatcher { populate(data) })
        populate(data)

        val bottom = card()
        bottom.addView(button("Export transactions as CSV") { exportCsv(data) })
        bottom.addView(button("Delete all local transaction data") {
            AlertDialog.Builder(this).setTitle("Delete all data?")
                .setMessage("This permanently deletes transactions saved on this phone.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ -> Store.clear(this); render() }.show()
        })
        content.addView(bottom)
        setContentView(root)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(13), dp(15), dp(13))
        setBackgroundColor(-1)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); layoutParams = lp
    }

    private fun populate(data: List<Tx>) {
        if (!::listBox.isInitialized) return
        listBox.removeAllViews()
        val q = if (::search.isInitialized) search.text.toString().trim().lowercase() else ""
        if (showPeople) {
            val grouped = data.groupBy { it.person }.map { (person, items) ->
                val sent = items.filter { it.direction == "OUT" }.sumOf { it.amount }
                val received = items.filter { it.direction == "IN" }.sumOf { it.amount }
                Triple(person, items, Pair(sent, received))
            }.filter { it.first.lowercase().contains(q) || it.second.any { t -> t.raw.lowercase().contains(q) } }
                .sortedByDescending { it.second.size }
            grouped.forEach { (person, items, totals) ->
                val c = card()
                c.addView(label(person, 16f, true))
                c.addView(label("Total transactions: ${items.size}", 13f))
                c.addView(label("Money sent: ${money(totals.first)}", 13f).apply { setTextColor(0xFFC34350.toInt()) })
                c.addView(label("Money received: ${money(totals.second)}", 13f).apply { setTextColor(0xFF12845F.toInt()) })
                c.addView(label("Net received: ${money(totals.second - totals.first)}", 13f, true))
                listBox.addView(c)
            }
        } else {
            val filtered = data.filter { listOf(it.person, it.raw, it.app, it.direction).any { s -> s.lowercase().contains(q) } }
            filtered.forEach { t ->
                val c = card()
                c.addView(label("${if (t.direction == "IN") "Received from" else "Sent to"} ${t.person}", 15f, true))
                c.addView(label("${money(t.amount)}  •  ${t.app}", 14f, true))
                c.addView(label(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t.timestamp)), 11f))
                c.addView(label(t.raw, 11f))
                listBox.addView(c)
            }
        }
        if (listBox.childCount == 0) listBox.addView(label(if (data.isEmpty()) "No transactions detected yet. Enable notification access, then make or receive a test payment notification." else "No matching records.", 13f))
    }

    private fun exportCsv(data: List<Tx>) {
        val csv = buildString {
            append("date,direction,amount,person,app,notification\n")
            data.forEach { t ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(t.timestamp))
                val cols = listOf(date, t.direction, t.amount.toString(), t.person, t.app, t.raw)
                append(cols.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } + "\n")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "MoneyTrack transactions")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        startActivity(Intent.createChooser(intent, "Share transaction CSV"))
    }
}

object NotificationAccess {
    fun isEnabled(activity: Activity): Boolean {
        val flat = Settings.Secure.getString(activity.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(activity.packageName)
    }
}

class SimpleWatcher(val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
