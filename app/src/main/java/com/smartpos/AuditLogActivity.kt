package com.smartpos
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : AppCompatActivity() {

    data class AuditItem(val account: String, val dateTime: String, val action: String, val details: String, val type: String)

    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvCount: TextView
    private val allLogs = mutableListOf<AuditItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#FDF6FF"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#E8D5F2"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "📜 AUDIT LOG - SYSTEM ACTIVITIES"
        title.setTextColor(Color.parseColor("#4A148C"))
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "System")) ?: "System"
        val tvHead = TextView(this)
        tvHead.text = "Account: $cashier | Date: ${sdf.format(Date())} | ${android.os.Build.MODEL}"
        tvHead.setTextColor(Color.parseColor("#7B1FA2"))
        tvHead.textSize = 11f
        header.addView(tvHead)

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.parseColor("#F3E5F5"))
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "🔍 Search Account / Date / Action..."
        etSearch.setBackgroundResource(android.R.drawable.editbox_background)
        etSearch.setPadding(25,25,25,25)
        tvCount = TextView(this)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.parseColor("#8E24AA"))
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val colHeader = TextView(this)
        colHeader.text = "ACCOUNT | DATE & TIME | ACTION | DETAILS"
        colHeader.setBackgroundColor(Color.parseColor("#CE93D8"))
        colHeader.setTextColor(Color.WHITE)
        colHeader.setPadding(15,15,15,15)
        colHeader.setTypeface(null, Typeface.BOLD)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        scroll.layoutParams = lpScroll
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(10,10,10,10)
        scroll.addView(container)

        root.addView(header)
        root.addView(searchBox)
        root.addView(colHeader)
        root.addView(scroll)
        setContentView(root)

        loadLogs()
        render(allLogs)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                val filtered = if (q.isEmpty()) allLogs else allLogs.filter {
                    it.account.lowercase().contains(q) || it.dateTime.lowercase().contains(q) || it.action.lowercase().contains(q) || it.details.lowercase().contains(q)
                }
                render(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadLogs() {
        allLogs.clear()
        val posPrefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashierName = posPrefs.getString("cashier_name", posPrefs.getString("logged_user", "System")) ?: "System"
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val salesPrefs = getSharedPreferences("sales_history", MODE_PRIVATE)
        for ((k, v) in salesPrefs.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val date = parts.getOrNull(0) ?: sdfDate.format(Date())
                val prod = parts.getOrNull(2) ?: k
                allLogs.add(AuditItem(cashierName, "$date 12:00:00", "SALE", "Sold $prod - $k", "SALE"))
            } catch (_: Exception) {}
        }

        val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)
        for ((k, _) in custPrefs.all) {
            allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "CUSTOMER", "Added customer $k", "CUSTOMER"))
        }

        val takeHistory = getSharedPreferences("stock_take_history", MODE_PRIVATE)
        for ((k, _) in takeHistory.all) {
            allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "STOCK TAKE", "Saved: ${k.replace("_", " ")}", "STOCKTAKE"))
        }

        val priceHistory = getSharedPreferences("price_history", MODE_PRIVATE)
        for ((_, v) in priceHistory.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val code = parts.getOrNull(0) ?: ""
                val old = parts.getOrNull(2) ?: "0"
                val newP = parts.getOrNull(3) ?: "0"
                val dt = parts.getOrNull(4) ?: sdfFull.format(Date())
                allLogs.add(AuditItem(cashierName, dt, "PRICE CHANGE", "$code from $$old to $$newP", "PRICE"))
            } catch (_: Exception) {}
        }

        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        for ((k, v) in goodsPrefs.all) {
            try {
                val str = v.toString()
                if (str.contains("|")) {
                    val parts = str.split("|")
                    val code = parts.getOrNull(0) ?: k
                    val name = parts.getOrNull(1) ?: k
                    val qty = parts.getOrNull(2) ?: "0"
                    allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "STOCK", "Stock: $code - $name Qty:$qty", "STOCK"))
                }
            } catch (_: Exception) {}
        }

        if (allLogs.isEmpty()) {
            allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "LOGIN", "User $cashierName logged in - ${android.os.Build.MODEL}", "USER"))
        }
        allLogs.sortByDescending { it.dateTime }
    }

    private fun render(list: List<AuditItem>) {
        container.removeAllViews()
        for (entry in list) {
            val bgColor = when (entry.type) {
                "SALE" -> Color.parseColor("#E8F5E9")
                "CUSTOMER" -> Color.parseColor("#E3F2FD")
                "STOCK" -> Color.parseColor("#FFF3E0")
                "STOCKTAKE" -> Color.parseColor("#F3E5F5")
                "PRICE" -> Color.parseColor("#FFF9C4")
                else -> Color.WHITE
            }

            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(bgColor)
            card.setPadding(15,12,15,12)
            val lpCard = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lpCard.setMargins(0,0,0,10)
            card.layoutParams = lpCard

            val line1 = TextView(this)
            line1.text = "👤 ${entry.account} | 🕒 ${entry.dateTime} | ${entry.action}"
            line1.textSize = 11f
            line1.setTypeface(null, Typeface.BOLD)
            line1.setTextColor(Color.parseColor("#4A148C"))

            val line2 = TextView(this)
            line2.text = "📌 ${entry.details}"
            line2.textSize = 13f
            line2.setTextColor(Color.parseColor("#424242"))
            line2.setPadding(0,6,0,0)

            card.addView(line1)
            card.addView(line2)
            container.addView(card)
        }
        tvCount.text = "${list.size} activities | Sales:${allLogs.count{it.type=="SALE"}} Cust:${allLogs.count{it.type=="CUSTOMER"}} Price:${allLogs.count{it.type=="PRICE"}}"
    }
}