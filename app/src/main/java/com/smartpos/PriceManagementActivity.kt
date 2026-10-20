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
    data class PStock(val code: String, val name: String, val qty: Int, val cost: Double, val selling: Double)
    data class PEntry(val stock: PStock, var newPrice: Double? = null)
    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvSummary: TextView
    private lateinit var tvCount: TextView
    private val allEntries = mutableListOf<PEntry>()
    private var filteredEntries = mutableListOf<PEntry>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F5F5")); layoutParams = ViewGroup.LayoutParams(-1,-1) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#4A148C")); setPadding(30,25,30,25) }
        val title = TextView(this).apply { text = "PRICE MANAGEMENT - LIVE %"; setTextColor(Color.WHITE); textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier"))
        val tvHead = TextView(this).apply { text = "Cashier: $cashier | ${sdf.format(Date())}"; setTextColor(Color.parseColor("#E1BEE7")); textSize = 11f }
        header.addView(title); header.addView(tvHead)
        val searchBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(15,15,15,15) }
        etSearch = EditText(this).apply { hint = "🔍 Search by Code or Description..."; setBackgroundResource(android.R.drawable.editbox_background); setPadding(25,25,25,25) }
        tvCount = TextView(this).apply { textSize = 12f; setTextColor(Color.GRAY) }
        searchBox.addView(etSearch); searchBox.addView(tvCount)
        val summaryBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F3E5F5")); setPadding(15,15,15,15) }
        tvSummary = TextView(this).apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) }
        val legend = TextView(this).apply { text = "🔴 Low <0% RED | 🟢 Good 0-50% GREEN | 🔵 Abnormal >50% BLUE"; textSize = 11f }
        summaryBox.addView(tvSummary); summaryBox.addView(legend)
        val listHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#37474F")); setPadding(10,12,10,12) }
        fun addH(t: String, w: Float) { val tv = TextView(this@PriceManagementActivity).apply { text=t; setTextColor(Color.WHITE); textSize=11f; typeface=Typeface.DEFAULT_BOLD; layoutParams=LinearLayout.LayoutParams(0,-2,w) }; listHeader.addView(tv) }
        addH("PRODUCT",1.6f); addH("CODE",0.8f); addH("QTY",0.5f); addH("OLD",0.8f); addH("NEW",0.9f); addH("%",1f)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1,0,1f) }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(5,5,5,5) }
        scroll.addView(container)
        val btnSave = Button(this).apply { text="💾 SAVE NEW PRICES"; setBackgroundColor(Color.parseColor("#4A148C")); setTextColor(Color.WHITE) }
        root.addView(header); root.addView(searchBox); root.addView(summaryBox); root.addView(listHeader); root.addView(scroll); root.addView(btnSave)
        setContentView(root)
        load(); render(allEntries)
        btnSave.setOnClickListener { save() }
        etSearch.addTextChangedListener(object: TextWatcher{ override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){} override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){ val q=s.toString().lowercase(); filteredEntries = if(q.isEmpty()) allEntries.toMutableList() else allEntries.filter{ it.stock.code.lowercase().contains(q) || it.stock.name.lowercase().contains(q)}.toMutableList(); render(filteredEntries)} override fun afterTextChanged(s: Editable?){} })
    }
    private fun load(){ allEntries.clear(); val combined = mutableMapOf<String,Any?>(); for(name in listOf("goods_received","products","stock_data")){ val p=getSharedPreferences(name,MODE_PRIVATE); for((k,v) in p.all) combined[k]=v }; for((k,v) in combined){ try{ val s=v.toString(); if(s.contains("|")){ val parts=s.split("|"); val code=parts.getOrNull(0)?:k; val nm=parts.getOrNull(1)?:k; val qty=parts.getOrNull(2)?.toIntOrNull()?:0; val cost=parts.getOrNull(3)?.toDoubleOrNull()?:1.0; val sell=parts.getOrNull(4)?.toDoubleOrNull()?:cost*1.3; allEntries.add(PEntry(PStock(code,nm,qty,cost,sell),null)) } }catch(_:Exception){} }; filteredEntries = allEntries.toMutableList() }
    private fun render(list: List<PEntry>){ container.removeAllViews(); for(entry in list){ val row=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,6); layoutParams=lp }; val tvP=TextView(this).apply{ text=entry.stock.name; textSize=11f; typeface=Typeface.DEFAULT_BOLD; layoutParams=LinearLayout.LayoutParams(0,-2,1.6f)}; val tvC=TextView(this).apply{ text=entry.stock.code; textSize=11f; setTextColor(Color.parseColor("#1976D2")); layoutParams=LinearLayout.LayoutParams(0,-2,0.8f)}; val tvQ=TextView(this).apply{ text="${entry.stock.qty}"; gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)}; val tvOld=TextView(this).apply{ text="$${String.format("%.2f",entry.stock.selling)}"; gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,0.8f)}; val etNew=EditText(this).apply{ textSize=12f; setBackgroundResource(android.R.drawable.editbox_background); setPadding(10,10,10,10); layoutParams=LinearLayout.LayoutParams(0,-2,0.9f).apply{setMargins(5,0,5,0)}; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; if(entry.newPrice!=null) setText("${entry.newPrice}") }; val tvPerc=TextView(this).apply{ textSize=12f; typeface=Typeface.DEFAULT_BOLD; gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setBackgroundColor(Color.parseColor("#EEEEEE")); setPadding(5,8,5,8)}; fun upd(){ val ns=etNew.text.toString().trim(); if(ns.isEmpty()){ entry.newPrice=null; tvPerc.text=""; tvPerc.setBackgroundColor(Color.parseColor("#EEEEEE")); updateSummary(); return }; val nv=ns.toDoubleOrNull()?:return; entry.newPrice=nv; val old=entry.stock.selling; if(old==0.0) return; val perc=((nv-old)/old)*100.0; when{ perc<-20 -> {tvPerc.setBackgroundColor(Color.parseColor("#90CAF9")); tvPerc.setTextColor(Color.parseColor("#0D47A1")); tvPerc.text="${String.format("%.1f",perc)}% ABNORMAL"}; perc<0 -> {tvPerc.setBackgroundColor(Color.parseColor("#FFCDD2")); tvPerc.setTextColor(Color.parseColor("#B71C1C")); tvPerc.text="${String.format("%.1f",perc)}% LOW"}; perc<=50 -> {tvPerc.setBackgroundColor(Color.parseColor("#C8E6C9")); tvPerc.setTextColor(Color.parseColor("#2E7D32")); tvPerc.text="+${String.format("%.1f",perc)}% GOOD"}; else -> {tvPerc.setBackgroundColor(Color.parseColor("#BBDEFB")); tvPerc.setTextColor(Color.parseColor("#0D47A1")); tvPerc.text="+${String.format("%.1f",perc)}% HIGH"} }; updateSummary() }; etNew.addTextChangedListener(object: TextWatcher{ override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){} override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){ upd() } override fun afterTextChanged(s: Editable?){} }); row.addView(tvP); row.addView(tvC); row.addView(tvQ); row.addView(tvOld); row.addView(etNew); row.addView(tvPerc); container.addView(row) }; tvCount.text="${list.size} products"; updateSummary() }
    private fun updateSummary(){ var changed=0; var low=0; var good=0; var ab=0; for(e in allEntries){ if(e.newPrice!=null){ changed++; val perc=((e.newPrice!!-e.stock.selling)/e.stock.selling)*100.0; when{ perc<0->low++; perc<=50->good++; else->ab++ } } }; tvSummary.text="Changed:$changed | 🔴Low:$low | 🟢Good:$good | 🔵Ab:$ab" }
    private fun save(){ if(allEntries.none{it.newPrice!=null}){Toast.makeText(this,"Enter price",Toast.LENGTH_SHORT).show(); return}; val gEdit=getSharedPreferences("goods_received",MODE_PRIVATE).edit(); val sEdit=getSharedPreferences("stock_data",MODE_PRIVATE).edit(); val pEdit=getSharedPreferences("products",MODE_PRIVATE).edit(); val hPrefs=getSharedPreferences("price_history",MODE_PRIVATE); val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(Date()); var saved=0; for(e in allEntries){ if(e.newPrice!=null){ val v="${e.stock.code}|${e.stock.name}|${e.stock.qty}|${e.stock.cost}|${e.newPrice}"; gEdit.putString(e.stock.code,v); sEdit.putString(e.stock.code,v); pEdit.putString(e.stock.code,v); hPrefs.edit().putString("${e.stock.code}_$date","${e.stock.code}|${e.stock.name}|${e.stock.selling}|${e.newPrice}|$date").apply(); saved++ } }; gEdit.apply(); sEdit.apply(); pEdit.apply(); Toast.makeText(this,"Saved $saved prices",Toast.LENGTH_LONG).show(); load(); render(allEntries) }
}