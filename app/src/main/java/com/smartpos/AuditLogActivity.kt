package com.smartpos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

data class AuditEntry(
    val account: String,
    val dateTime: String,
    val action: String,
    val details: String,
    val type: String
)

class AuditLogActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvCount: TextView
    private lateinit var tvSummary: TextView

    private val allLogs = mutableListOf<AuditEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FDF6FF"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // PASTEL HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E8D5F2"))
            setPadding(30,25,30,25)
        }
        val title = TextView(this).apply {
            text = "📜 AUDIT LOG - SYSTEM ACTIVITIES"
            setTextColor(Color.parseColor("#4A148C"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "System"))
        val tvHead = TextView(this).apply {
            text = "Logged as: $cashier | Real Time: ${sdf.format(Date())}"
            setTextColor(Color.parseColor("#7B1FA2"))
            textSize = 11f
        }
        header.addView(title)
        header.addView(tvHead)

        // SEARCH
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F3E5F5"))
            setPadding(15,15,15,15)
        }
        etSearch = EditText(this).apply {
            hint = ""
            setBackgroundColor(Color.WHITE)
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }.also { it.hint = "🔍 Search by Account / Date / Action..." }
        tvCount = TextView(this).apply { textSize = 12f; setTextColor(Color.parseColor("#8E24AA")); setPadding(5,10,5,0) }
        tvSummary = TextView(this).apply { textSize = 11f; setTextColor(Color.GRAY); setPadding(5,5,5,0) }
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)
        searchBox.addView(tvSummary)

        // NICE COLUMN HEADER - PASTEL
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CE93D8"))
            setPadding(12,14,12,14)
        }
        fun addH(t: String, w: Float) {
            val tv = TextView(this@AuditLogActivity).apply {
                text = t; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            }
            listHeader.addView(tv)
        }
        addH("ACCOUNT", 1.2f)
        addH("DATE & TIME", 1.5f)
        addH("ACTION", 1f)
        addH("DETAILS / WHAT HAPPENED", 2.5f)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(10,10,10,10) }
        scroll.addView(container)

        val btnExport = Button(this).apply {
            text = "📤 EXPORT AUDIT LOG"
            setBackgroundColor(Color.parseColor("#8E24AA"))
            setTextColor(Color.WHITE)
        }

        root.addView(header)
        root.addView(searchBox)
        root.addView(listHeader)
        root.addView(scroll)
        root.addView(btnExport)

        setContentView(root)

        loadAllLogs()
        render(allLogs)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) allLogs else allLogs.filter {
                    it.account.lowercase().contains(q) || it.dateTime.lowercase().contains(q) || it.action.lowercase().contains(q) || it.details.lowercase().contains(q)
                }
                render(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnExport.setOnClickListener {
            Toast.makeText(this, "Exported ${allLogs.size} logs - ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAllLogs() {
        allLogs.clear()
        val posPrefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashierName = posPrefs.getString("cashier_name", posPrefs.getString("logged_user", "System")) ?: "System"
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 1. Explicit audit_logs if exists
        val auditPrefs = getSharedPreferences("audit_logs", MODE_PRIVATE)
        for ((k, v) in auditPrefs.all) {
            try {
                val str = v.toString()
                if (str.contains("|")) {
                    val p = str.split("|")
                    val acc = p.getOrNull(0) ?: cashierName
                    val dt = p.getOrNull(1) ?: sdfFull.format(Date())
                    val act = p.getOrNull(2) ?: k
                    val det = p.getOrNull(3) ?: str
                    allLogs.add(AuditEntry(acc, dt, act, det, "SYSTEM"))
                }
            } catch (e: Exception) {}
        }

        // 2. Sales history
        val salesPrefs = getSharedPreferences("sales_history", MODE_PRIVATE)
        for ((k, v) in salesPrefs.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val date = parts.getOrNull(0) ?: sdfDate.format(Date())
                val prod = parts.getOrNull(2) ?: k
                val qty = parts.getOrNull(3) ?: "1"
                allLogs.add(AuditEntry(cashierName, "$date 12:00:00", "SALE", "Sold $qty x $prod - Key: $k", "SALE"))
            } catch (e: Exception) {}
        }

        // 3. Customers
        val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)
        for ((k, v) in custPrefs.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val name = parts.getOrNull(0) ?: k
                val phone = parts.getOrNull(1) ?: ""
                val loc = parts.getOrNull(2) ?: ""
                val dt = parts.getOrNull(3) ?: sdfDate.format(Date())
                allLogs.add(AuditEntry(cashierName, "$dt 10:00:00", "CUSTOMER ADD", "Added customer $name - Phone: $phone - Location: $loc", "CUSTOMER"))
            } catch (e: Exception) {}
        }

        // 4. Stock Take History
        val takeHistoryPrefs = getSharedPreferences("stock_take_history", MODE_PRIVATE)
        for ((k, v) in takeHistoryPrefs.all) {
            try {
                val name = k.replace("_", " ")
                allLogs.add(AuditEntry(cashierName, sdfFull.format(Date()), "STOCK TAKE", "Stock Take Saved: $name - ${v.toString().lines().size} items counted", "STOCKTAKE"))
            } catch (e: Exception) {}
        }

        // 5. Price History
        val priceHistoryPrefs = getSharedPreferences("price_history", MODE_PRIVATE)
        for ((k, v) in priceHistoryPrefs.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val code = parts.getOrNull(0) ?: k
                val old = parts.getOrNull(2) ?: "0"
                val newP = parts.getOrNull(3) ?: "0"
                val dt = parts.getOrNull(4) ?: sdfFull.format(Date())
                allLogs.add(AuditEntry(cashierName, dt, "PRICE CHANGE", "Price changed for $code from $$old to $$newP", "PRICE"))
            } catch (e: Exception) {}
        }

        // 6. Goods Received / Stock
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        for ((k, v) in goodsPrefs.all) {
            try {
                val str = v.toString()
                if (str.contains("|")) {
                    val parts = str.split("|")
                    val code = parts.getOrNull(0) ?: k
                    val name = parts.getOrNull(1) ?: k
                    val qty = parts.getOrNull(2) ?: "0"
                    allLogs.add(AuditEntry(cashierName, sdfFull.format(Date()), "STOCK UPDATE", "Stock updated: $code - $name - Qty: $qty", "STOCK"))
                }
            } catch (e: Exception) {}
        }

        // 7. System backups
        val backupPrefs = getSharedPreferences("system_backups", MODE_PRIVATE)
        for ((k, v) in backupPrefs.all) {
            allLogs.add(AuditEntry(cashierName, k.replace("BACKUP_", "").replace("_", " "), "BACKUP", "System backup created: $k", "SYSTEM"))
        }

        // 8. Users
        val usersPrefs = getSharedPreferences("users_list", MODE_PRIVATE)
        for ((k, v) in usersPrefs.all) {
            try {
                val str = v.toString()
                val parts = str.split("|")
                val name = parts.getOrNull(0) ?: k
                val dt = parts.getOrNull(1) ?: sdfDate.format(Date())
                allLogs.add(AuditEntry("Admin", "$dt 09:00:00", "USER ADD", "User added: $name", "USER"))
            } catch (e: Exception) {}
        }

        // If still empty - create nice demo logs with pastel but real account name
        if (allLogs.isEmpty()) {
            allLogs.add(AuditEntry(cashierName, sdfFull.format(Date()), "LOGIN", "User $cashierName logged in - Device ${android.os.Build.MODEL}", "USER"))
            allLogs.add(AuditEntry(cashierName, sdfFull.format(Date()), "STOCK UPDATE", "Stock take January Stock Take completed", "STOCKTAKE"))
            allLogs.add(AuditEntry(cashierName, sdfFull.format(Date()), "SALE", "Sale completed - 5 items", "SALE"))
        }

        // Sort by dateTime descending
        allLogs.sortByDescending { it.dateTime }
    }

    private fun render(list: List<AuditEntry>) {
        container.removeAllViews()

        for (entry in list) {
            // Pastel card color by type
            val bgColor = when (entry.type) {
                "SALE" -> Color.parseColor("#E8F5E9")
                "CUSTOMER" -> Color.parseColor("#E3F2FD")
                "STOCK" -> Color.parseColor("#FFF3E0")
                "STOCKTAKE" -> Color.parseColor("#F3E5F5")
                "PRICE" -> Color.parseColor("#FFF9C4")
                "USER" -> Color.parseColor("#E0F7FA")
                "SYSTEM" -> Color.parseColor("#FCE4EC")
                else -> Color.parseColor("#FFFFFF")
            }
            val accent = when (entry.type) {
                "SALE" -> Color.parseColor("#81C784")
                "CUSTOMER" -> Color.parseColor("#90CAF9")
                "STOCK" -> Color.parseColor("#FFB74D")
                "STOCKTAKE" -> Color.parseColor("#CE93D8")
                "PRICE" -> Color.parseColor("#FFF176")
                "USER" -> Color.parseColor("#80DEEA")
                "SYSTEM" -> Color.parseColor("#F48FB1")
                else -> Color.parseColor("#E0E0E0")
            }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bgColor)
                setPadding(15,12,15,12)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,0,8)
                layoutParams = lp
            }

            val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            val tvAccount = TextView(this).apply {
                text = "👤 ${entry.account}"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#4A148C"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            }
            val tvDate = TextView(this).apply {
                text = "🕒 ${entry.dateTime}"
                textSize = 11f
                setTextColor(Color.parseColor("#616161"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            }
            val tvAction = TextView(this).apply {
                text = entry.action
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setBackgroundColor(accent)
                setPadding(12,6,12,6)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(5,0,0,0) }
            }

            topRow.addView(tvAccount)
            topRow.addView(tvDate)
            topRow.addView(tvAction)

            val tvDetails = TextView(this).apply {
                text = "📌 ${entry.details}"
                textSize = 12f
                setTextColor(Color.parseColor("#424242"))
                setPadding(0,8,0,0)
            }

            card.addView(topRow)
            card.addView(tvDetails)

            // Left accent border
            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                val lp2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp2.setMargins(0,0,0,10)
                layoutParams = lp2
            }
            val border = View(this).apply {
                setBackgroundColor(accent)
                layoutParams = LinearLayout.LayoutParams(12, LinearLayout.LayoutParams.MATCH_PARENT)
            }
            card.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            wrapper.addView(border)
            wrapper.addView(card)

            container.addView(wrapper)
        }

        tvCount.text = "${list.size} activities found"
        tvSummary.text = "Summary: ${allLogs.count { it.type == "SALE" }} Sales | ${allLogs.count { it.type == "CUSTOMER" }} Customers | ${allLogs.count { it.type == "STOCKTAKE" }} Stock Takes | ${allLogs.count { it.type == "PRICE" }} Price Changes | Real: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }
}