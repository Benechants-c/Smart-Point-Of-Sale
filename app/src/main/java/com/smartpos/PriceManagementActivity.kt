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

class PriceManagementActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvSummary: TextView
    private lateinit var tvCount: TextView
    private val allEntries = mutableListOf<PriceEntry>()
    private var filteredEntries = mutableListOf<PriceEntry>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F5F5")); layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#4A148C")); setPadding(30,25,30,25) }
        val title = TextView(this).apply { text = "PRICE MANAGEMENT - LIVE % CHECK"; setTextColor(Color.WHITE); textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier"))
        val tvHead = TextView(this).apply { text = "Cashier: $cashier | Real: ${sdf.format(Date())}"; setTextColor(Color.parseColor("#E1BEE7")); textSize = 11f }
        header.addView(title); header.addView(tvHead)
        val searchBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(15,15,15,15) }
        etSearch = EditText(this).apply { hint = "🔍 Search by Code or Description..."; setBackgroundResource(android.R.drawable.editbox_background); setPadding(25,25,25,25) }
        tvCount = TextView(this).apply { textSize = 12f; setTextColor(Color.GRAY); setPadding(5,10,5,0) }
        searchBox.addView(etSearch); searchBox.addView(tvCount)
        val summaryBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F3E5F5")); setPadding(15,15,15,15) }
        tvSummary = TextView(this).apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) }
        val legend = TextView(this).apply { text = "🔴 Low <0% | 🟢 Good 0-50% | 🔵 Abnormal >50% or <-20%"; textSize = 11f; setTextColor(Color.DKGRAY); setPadding(0,8,0,0) }
        summaryBox.addView(tvSummary); summaryBox.addView(legend)
        val listHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#37474F")); setPadding(10,12,10,12) }
        fun addH(t: String, w: Float) { val tv = TextView(this@PriceManagementActivity).apply { text = t; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w) }; listHeader.addView(tv) }
        addH("PRODUCT", 1.6f); addH("CODE", 0.8f); addH("QTY", 0.5f); addH("OLD", 0.8f); addH("NEW", 0.9f); addH("%", 1f)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(5,5,5,5) }
        scroll.addView(container)
        val btnSave = Button(this).apply { text = "💾 SAVE NEW PRICES"; setBackgroundColor(Color.parseColor("#4A148C")); setTextColor(Color.WHITE) }
        root.addView(header); root.addView(searchBox); root.addView(summaryBox); root.addView(listHeader); root.addView(scroll); root.addView(btnSave)
        setContentView(root)
        loadFromStockList(); render(allEntries)
        btnSave.setOnClickListener { savePrices() }
        etSearch.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { val q = s.toString().trim().lowercase(); filteredEntries = if (q.isEmpty()) allEntries.toMutableList() else allEntries.filter { it.stockItem.code.lowercase().contains(q) || it.stockItem.name.lowercase().contains(q) }.toMutableList(); render(filteredEntries) }; override fun afterTextChanged(s: Editable?) {} })
    }
    private fun loadFromStockList() {
        allEntries.clear(); val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE); val productsPrefs = getSharedPreferences("products", MODE_PRIVATE); val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)
        val combined = mutableMapOf<String, Any?>(); for ((k,v) in goodsPrefs.all) combined[k]=v; for ((k,v) in productsPrefs.all) combined[k]=v; for ((k,v) in stockPrefs.all) combined[k]=v
        val tempStock = mutableListOf<StockItem>()
        if (combined.isNotEmpty()) { for ((key,value) in combined) { try { val str = value.toString(); if (str.contains("|")) { val parts=str.split("|"); val code=parts.getOrNull(0)?:key; val name=parts.getOrNull(1)?:key; val qty=parts.getOrNull(2)?.toIntOrNull()?:0; val cost=parts.getOrNull(3)?.toDoubleOrNull()?:1.0; val sell=parts.getOrNull(4)?.toDoubleOrNull()?:cost*1.3; tempStock.add(StockItem(code,name,qty,cost,sell)) } } catch(e: Exception){} } }
        for (item in tempStock) allEntries.add(PriceEntry(item, null)); filteredEntries = allEntries.toMutableList()
    }
    private fun render(list: List<PriceEntry>) {
        container.removeAllViews()
        for (entry in list) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10); val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,0,6); layoutParams = lp }
            val tvProduct = TextView(this).apply { text = entry.stockItem.name; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.6f) }
            val tvCode = TextView(this).apply { text = entry.stockItem.code; textSize = 11f; setTextColor(Color.parseColor("#1976D2")); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
            val tvQty = TextView(this).apply { text = "${entry.stockItem.qty}"; textSize = 12f; setTextColor(Color.BLACK); typeface = Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f) }
            val tvOld = TextView(this).apply { text = "$${String.format("%.2f", entry.stockItem.selling)}"; textSize = 12f; setTextColor(Color.BLACK); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f); gravity = android.view.Gravity.CENTER }
            val etNew = EditText(this).apply { hint=""; textSize=12f; setBackgroundResource(android.R.drawable.editbox_background); setPadding(10,10,10,10); layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply{setMargins(5,0,5,0)}; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; if (entry.newPrice!=null) setText("${entry.newPrice}") }
            val tvPercent = TextView(this).apply { textSize=12f; typeface=Typeface.DEFAULT_BOLD; gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setPadding(5,8,5,8); text=""; setBackgroundColor(Color.parseColor("#EEEEEE")) }
            fun updatePercent() { val newStr=etNew.text.toString().trim(); if (newStr.isEmpty()) { entry.newPrice=null; tvPercent.text=""; tvPercent.setBackgroundColor(Color.parseColor("#EEEEEE")); tvPercent.setTextColor(Color.GRAY); updateSummary(); return }; val newVal=newStr.toDoubleOrNull(); if (newVal==null) return; entry.newPrice=newVal; val old=entry.stockItem.selling; if (old==0.0) return; val percent=((newVal-old)/old)*100.0; when { percent<-20.0 -> {tvPercent.setBackgroundColor(Color.parseColor("#90CAF9")); tvPercent.setTextColor(Color.parseColor("#0D47A1")); tvPercent.text="${String.format("%.1f", percent)}% ABNORMAL"}; percent<0 -> {tvPercent.setBackgroundColor(Color.parseColor("#FFCDD2")); tvPercent.setTextColor(Color.parseColor("#B71C1C")); tvPercent.text="${String.format("%.1f", percent)}% LOW"}; percent<=50.0 -> {tvPercent.setBackgroundColor(Color.parseColor("#C8E6C9")); tvPercent.setTextColor(Color.parseColor("#2E7D32")); tvPercent.text="+${String.format("%.1f", percent)}% GOOD"}; else -> {tvPercent.setBackgroundColor(Color.parseColor("#BBDEFB")); tvPercent.setTextColor(Color.parseColor("#0D47A1")); tvPercent.text="+${String.format("%.1f", percent)}% HIGH"} }; updateSummary() }
            etNew.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePercent() }; override fun afterTextChanged(s: Editable?) {} })
            row.addView(tvProduct); row.addView(tvCode); row.addView(tvQty); row.addView(tvOld); row.addView(etNew); row.addView(tvPercent); container.addView(row)
        }
        tvCount.text="${list.size} products"; updateSummary()
    }
    private fun updateSummary() { var changed=0; var low=0; var good=0; var abnormal=0; for (e in allEntries) { if (e.newPrice!=null) { changed++; val old=e.stockItem.selling; val perc=((e.newPrice!!-old)/old)*100.0; when { perc<0->low++; perc<=50.0->good++; else->abnormal++ } } }; tvSummary.text="Changed: $changed | 🔴 Low: $low | 🟢 Good: $good | 🔵 Abnormal: $abnormal" }
    private fun savePrices() { val hasChange=allEntries.any{it.newPrice!=null}; if (!hasChange) {Toast.makeText(this, "Enter new price", Toast.LENGTH_SHORT).show(); return}; val goodsPrefs=getSharedPreferences("goods_received", MODE_PRIVATE); val stockPrefs=getSharedPreferences("stock_data", MODE_PRIVATE); val productsPrefs=getSharedPreferences("products", MODE_PRIVATE); val goodsEdit=goodsPrefs.edit(); val stockEdit=stockPrefs.edit(); val prodEdit=productsPrefs.edit(); var saved=0; val historyPrefs=getSharedPreferences("price_history", MODE_PRIVATE); val dateStr=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()); for (e in allEntries) { if (e.newPrice!=null) { val code=e.stockItem.code; val name=e.stockItem.name; val qty=e.stockItem.qty; val cost=e.stockItem.cost; val newSell=e.newPrice!!; val value="$code|$name|$qty|$cost|$newSell"; goodsEdit.putString(code,value); stockEdit.putString(code,value); prodEdit.putString(code,value); val key="${code}_$dateStr"; historyPrefs.edit().putString(key, "$code|${e.stockItem.name}|${e.stockItem.selling}|$newSell|$dateStr").apply(); saved++ } }; goodsEdit.apply(); stockEdit.apply(); prodEdit.apply(); Toast.makeText(this, "Saved $saved new prices", Toast.LENGTH_LONG).show(); loadFromStockList(); render(allEntries) }
}