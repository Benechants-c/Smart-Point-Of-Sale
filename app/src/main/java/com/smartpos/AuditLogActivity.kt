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
class AuditLogActivity : AppCompatActivity() {
    data class AuditEntryL(val account: String, val dateTime: String, val action: String, val details: String, val type: String)
    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvCount: TextView
    private lateinit var tvSummary: TextView
    private val allLogs = mutableListOf<AuditEntryL>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#FDF6FF")); layoutParams=ViewGroup.LayoutParams(-1,-1)}
        val header = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#E8D5F2")); setPadding(30,25,30,25)}
        val title = TextView(this).apply{ text="📜 AUDIT LOG - SYSTEM ACTIVITIES"; setTextColor(Color.parseColor("#4A148C")); textSize=18f; typeface=Typeface.DEFAULT_BOLD}
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs=getSharedPreferences("pos_prefs",MODE_PRIVATE)
        val cashier=prefs.getString("cashier_name",prefs.getString("logged_user","System"))
        val tvHead=TextView(this).apply{ text="Logged as: $cashier | ${sdf.format(Date())}"; setTextColor(Color.parseColor("#7B1FA2")); textSize=11f}
        header.addView(title); header.addView(tvHead)
        val searchBox=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F3E5F5")); setPadding(15,15,15,15)}
        etSearch=EditText(this).apply{ hint="🔍 Search by Account / Date / Action..."; setBackgroundResource(android.R.drawable.editbox_background); setPadding(25,25,25,25)}
        tvCount=TextView(this).apply{ textSize=12f; setTextColor(Color.parseColor("#8E24AA"))}
        tvSummary=TextView(this).apply{ textSize=11f; setTextColor(Color.GRAY)}
        searchBox.addView(etSearch); searchBox.addView(tvCount); searchBox.addView(tvSummary)
        val listHeader=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#CE93D8")); setPadding(12,14,12,14)}
        fun addH(t:String,w:Float){ val tv=TextView(this@AuditLogActivity).apply{ text=t; setTextColor(Color.WHITE); textSize=11f; typeface=Typeface.DEFAULT_BOLD; layoutParams=LinearLayout.LayoutParams(0,-2,w)}; listHeader.addView(tv)}
        addH("ACCOUNT",1.2f); addH("DATE & TIME",1.5f); addH("ACTION",1f); addH("DETAILS",2.5f)
        val scroll=ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f)}
        container=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(10,10,10,10)}
        scroll.addView(container)
        root.addView(header); root.addView(searchBox); root.addView(listHeader); root.addView(scroll)
        setContentView(root)
        loadAll(); render(allLogs)
        etSearch.addTextChangedListener(object: TextWatcher{ override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){} override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){ val q=s.toString().lowercase(); val filtered=if(q.isEmpty()) allLogs else allLogs.filter{ it.account.lowercase().contains(q) || it.dateTime.lowercase().contains(q) || it.action.lowercase().contains(q) || it.details.lowercase().contains(q)}; render(filtered)} override fun afterTextChanged(s: Editable?){} })
    }
    private fun loadAll(){ allLogs.clear(); val posPrefs=getSharedPreferences("pos_prefs",MODE_PRIVATE); val cashierName=posPrefs.getString("cashier_name",posPrefs.getString("logged_user","System"))?:"System"; val sdfFull=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()); val sdfDate=SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()); val salesPrefs=getSharedPreferences("sales_history",MODE_PRIVATE); for((k,v) in salesPrefs.all){ try{ val str=v.toString(); val parts=str.split("|"); val date=parts.getOrNull(0)?:sdfDate.format(Date()); val prod=parts.getOrNull(2)?:k; allLogs.add(AuditEntryL(cashierName,"$date 12:00:00","SALE","Sold $prod - $k","SALE"))}catch(_:Exception){} }; val custPrefs=getSharedPreferences("customers_data",MODE_PRIVATE); for((k,v) in custPrefs.all){ try{ val str=v.toString(); val parts=str.split("|"); val name=parts.getOrNull(0)?:k; allLogs.add(AuditEntryL(cashierName,sdfFull.format(Date()),"CUSTOMER ADD","Added customer $name","CUSTOMER"))}catch(_:Exception){} }; val takeHistory=getSharedPreferences("stock_take_history",MODE_PRIVATE); for((k,_) in takeHistory.all){ allLogs.add(AuditEntryL(cashierName,sdfFull.format(Date()),"STOCK TAKE","Stock Take Saved: ${k.replace("_"," ")}","STOCKTAKE"))}; val priceHistory=getSharedPreferences("price_history",MODE_PRIVATE); for((_,v) in priceHistory.all){ try{ val str=v.toString(); val parts=str.split("|"); val code=parts.getOrNull(0)?:""; val old=parts.getOrNull(2)?:"0"; val newP=parts.getOrNull(3)?:"0"; val dt=parts.getOrNull(4)?:sdfFull.format(Date()); allLogs.add(AuditEntryL(cashierName,dt,"PRICE CHANGE","$code from $$old to $$newP","PRICE"))}catch(_:Exception){} }; val goodsPrefs=getSharedPreferences("goods_received",MODE_PRIVATE); for((k,v) in goodsPrefs.all){ try{ val str=v.toString(); if(str.contains("|")){ val parts=str.split("|"); val code=parts.getOrNull(0)?:k; val name=parts.getOrNull(1)?:k; val qty=parts.getOrNull(2)?:"0"; allLogs.add(AuditEntryL(cashierName,sdfFull.format(Date()),"STOCK UPDATE","Stock: $code - $name Qty:$qty","STOCK"))}}catch(_:Exception){} }; if(allLogs.isEmpty()){ allLogs.add(AuditEntryL(cashierName,sdfFull.format(Date()),"LOGIN","User $cashierName logged in - ${android.os.Build.MODEL}","USER"))}; allLogs.sortByDescending{ it.dateTime} }
    private fun render(list: List<AuditEntryL>){ container.removeAllViews(); for(entry in list){ val bgColor=when(entry.type){ "SALE"->Color.parseColor("#E8F5E9"); "CUSTOMER"->Color.parseColor("#E3F2FD"); "STOCK"->Color.parseColor("#FFF3E0"); "STOCKTAKE"->Color.parseColor("#F3E5F5"); "PRICE"->Color.parseColor("#FFF9C4"); else->Color.parseColor("#FFFFFF")}; val accent=when(entry.type){ "SALE"->Color.parseColor("#81C784"); "CUSTOMER"->Color.parseColor("#90CAF9"); "STOCK"->Color.parseColor("#FFB74D"); "STOCKTAKE"->Color.parseColor("#CE93D8"); "PRICE"->Color.parseColor("#FFF176"); else->Color.parseColor("#E0E0E0")}; val card=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(bgColor); setPadding(15,12,15,12); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,8); layoutParams=lp}; val topRow=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL}; val tvAcc=TextView(this).apply{ text="👤 ${entry.account}"; textSize=11f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#4A148C")); layoutParams=LinearLayout.LayoutParams(0,-2,1.2f)}; val tvDate=TextView(this).apply{ text="🕒 ${entry.dateTime}"; textSize=11f; setTextColor(Color.parseColor("#616161")); layoutParams=LinearLayout.LayoutParams(0,-2,1.5f)}; val tvAct=TextView(this).apply{ text=entry.action; textSize=10f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setBackgroundColor(accent); setPadding(12,6,12,6); layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(5,0,0,0)}}; topRow.addView(tvAcc); topRow.addView(tvDate); topRow.addView(tvAct); val tvDet=TextView(this).apply{ text="📌 ${entry.details}"; textSize=12f; setTextColor(Color.parseColor("#424242")); setPadding(0,8,0,0)}; card.addView(topRow); card.addView(tvDet); val wrapper=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); val lp2=LinearLayout.LayoutParams(-1,-2); lp2.setMargins(0,0,0,10); layoutParams=lp2}; val border=View(this).apply{ setBackgroundColor(accent); layoutParams=LinearLayout.LayoutParams(12,-1)}; card.layoutParams=LinearLayout.LayoutParams(0,-2,1f); wrapper.addView(border); wrapper.addView(card); container.addView(wrapper)}; tvCount.text="${list.size} activities"; tvSummary.text="Summary: ${allLogs.count{it.type=="SALE"}} Sales | ${allLogs.count{it.type=="CUSTOMER"}} Customers | Real: ${SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())}" }
}