package com.smartpos
import android.os.Bundle
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
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#FDF6FF"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#E8D5F2"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "AUDIT LOG - SYSTEM ACTIVITIES"
        title.setTextColor(Color.parseColor("#4A148C"))
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title)

        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", "System") ?: "System"

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "Search Account / Date / Action..."
        etSearch.setPadding(25,25,25,25)
        etSearch.setBackgroundColor(Color.WHITE)
        tvCount = TextView(this)
        tvCount.textSize = 12f
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val colHeader = TextView(this)
        colHeader.text = "ACCOUNT | DATE & TIME | ACTION | DETAILS"
        colHeader.setBackgroundColor(Color.parseColor("#CE93D8"))
        colHeader.setTextColor(Color.WHITE)
        colHeader.setPadding(15,15,15,15)
        colHeader.setTypeface(null, Typeface.BOLD)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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

        etSearch.addTextChangedListener(object : android.text.TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){
                val q = s.toString().lowercase()
                val filtered = if(q.isEmpty()) allLogs else allLogs.filter{ it.account.lowercase().contains(q) || it.dateTime.lowercase().contains(q) || it.action.lowercase().contains(q) || it.details.lowercase().contains(q) }
                render(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?){}
        })
        }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show(); finish() }
    }

    private fun loadLogs(){
        try{
        allLogs.clear()
        val posPrefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashierName = posPrefs.getString("cashier_name", "System") ?: "System"
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val salesPrefs = getSharedPreferences("sales_history", MODE_PRIVATE)
        for((k,v) in salesPrefs.all){ allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "SALE", "Sold $k - $v", "SALE")) }
        val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)
        for((k,_) in custPrefs.all){ allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "CUSTOMER", "Added $k", "CUSTOMER")) }
        val priceHistory = getSharedPreferences("price_history", MODE_PRIVATE)
        for((_,v) in priceHistory.all){ allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "PRICE", v.toString(), "PRICE")) }
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        for((k,v) in goodsPrefs.all){ allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "STOCK", "$k - $v", "STOCK")) }
        if(allLogs.isEmpty()){ allLogs.add(AuditItem(cashierName, sdfFull.format(Date()), "LOGIN", "User logged in", "USER")) }
        allLogs.sortByDescending { it.dateTime }
        }catch(_:Exception){}
    }

    private fun render(list: List<AuditItem>){
        try{
        container.removeAllViews()
        for(entry in list){
            val bgColor = when(entry.type){
                "SALE" -> Color.parseColor("#E8F5E9")
                "CUSTOMER" -> Color.parseColor("#E3F2FD")
                "STOCK" -> Color.parseColor("#FFF3E0")
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
            line1.text = "${entry.account} | ${entry.dateTime} | ${entry.action}"
            line1.textSize = 11f
            line1.setTypeface(null, Typeface.BOLD)
            val line2 = TextView(this)
            line2.text = entry.details
            line2.textSize = 12f
            card.addView(line1)
            card.addView(line2)
            container.addView(card)
        }
        tvCount.text = "${list.size} activities"
        }catch(_:Exception){}
    }
}