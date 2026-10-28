package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class PriceManagementActivity : Activity() {
    data class PStock(val code: String, val name: String, val qty: Int, val cost: Double, val selling: Double)
    data class PEntry(val stock: PStock, var newPrice: Double? = null)

    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvSummary: TextView
    private lateinit var tvCount: TextView
    private val allEntries = mutableListOf<PEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#4A148C"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "PRICE MANAGEMENT - LIVE % CHECK"
        title.setTextColor(Color.WHITE)
        title.textSize = 17f
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", "Cashier") ?: "Cashier"
        val tvHead = TextView(this)
        tvHead.text = "Cashier: $cashier | ${sdf.format(Date())}"
        tvHead.setTextColor(Color.parseColor("#E1BEE7"))
        tvHead.textSize = 11f
        header.addView(tvHead)

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "Search Code or Description..."
        etSearch.setPadding(25,25,25,25)
        etSearch.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tvCount = TextView(this)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.GRAY)
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summaryBox = LinearLayout(this)
        summaryBox.orientation = LinearLayout.VERTICAL
        summaryBox.setBackgroundColor(Color.parseColor("#F3E5F5"))
        summaryBox.setPadding(15,15,15,15)
        tvSummary = TextView(this)
        tvSummary.textSize = 12f
        tvSummary.setTypeface(null, Typeface.BOLD)
        val legend = TextView(this)
        legend.text = "Low <0% | Good 0-50% | High >50%"
        legend.textSize = 11f
        summaryBox.addView(tvSummary)
        summaryBox.addView(legend)

        val listHeader = LinearLayout(this)
        listHeader.orientation = LinearLayout.HORIZONTAL
        listHeader.setBackgroundColor(Color.parseColor("#37474F"))
        listHeader.setPadding(10,12,10,12)
        val h1 = TextView(this); h1.text="PRODUCT"; h1.setTextColor(Color.WHITE); h1.setTypeface(null,Typeface.BOLD)
        val h2 = TextView(this); h2.text="OLD"; h2.setTextColor(Color.WHITE); h2.setTypeface(null,Typeface.BOLD)
        val h3 = TextView(this); h3.text="NEW"; h3.setTextColor(Color.WHITE); h3.setTypeface(null,Typeface.BOLD)
        val h4 = TextView(this); h4.text="% CHECK"; h4.setTextColor(Color.WHITE); h4.setTypeface(null,Typeface.BOLD)
        h1.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
        h2.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        h3.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        h4.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
        listHeader.addView(h1); listHeader.addView(h2); listHeader.addView(h3); listHeader.addView(h4)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        scroll.layoutParams = lpScroll
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(5,5,5,5)
        scroll.addView(container)

        val btnSave = Button(this)
        btnSave.text = "SAVE NEW PRICES"
        btnSave.setBackgroundColor(Color.parseColor("#4A148C"))
        btnSave.setTextColor(Color.WHITE)

        root.addView(header)
        root.addView(searchBox)
        root.addView(summaryBox)
        root.addView(listHeader)
        root.addView(scroll)
        root.addView(btnSave)
        setContentView(root)

        loadData()
        render(allEntries)

        btnSave.setOnClickListener { savePrices() }

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){
                val q = s.toString().lowercase()
                val filtered = if(q.isEmpty()) allEntries else allEntries.filter{ it.stock.code.lowercase().contains(q) || it.stock.name.lowercase().contains(q) }
                render(filtered)
            }
            override fun afterTextChanged(s: Editable?){}
        })
        }catch(e:Exception){ Toast.makeText(this,"Init error: ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun loadData(){
        try{
        allEntries.clear()
        val combined = mutableMapOf<String, Any?>()
        for(name in listOf("goods_received","products","stock_data")){
            val p = getSharedPreferences(name, MODE_PRIVATE)
            for((k,v) in p.all) combined[k]=v
        }
        for((k,v) in combined){
            try{
                val s = v.toString()
                if(s.contains("|")){
                    val parts = s.split("|")
                    val code = parts.getOrNull(0)?:k
                    val nm = parts.getOrNull(1)?:k
                    val qty = parts.getOrNull(2)?.toIntOrNull()?:0
                    val cost = parts.getOrNull(3)?.toDoubleOrNull()?:1.0
                    val sell = parts.getOrNull(4)?.toDoubleOrNull()?:cost*1.3
                    allEntries.add(PEntry(PStock(code,nm,qty,cost,sell), null))
                }
            }catch(_:Exception){}
        }
        if(allEntries.isEmpty()){
            allEntries.add(PEntry(PStock("001","Sample Product",10,5.0,7.0), null))
        }
        }catch(_:Exception){ allEntries.add(PEntry(PStock("001","Sample Product",10,5.0,7.0), null)) }
    }

    private fun render(list: List<PEntry>){
        try{
        container.removeAllViews()
        for(entry in list){
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(10,10,10,10)
            val lpRow = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lpRow.setMargins(0,0,0,6)
            row.layoutParams = lpRow

            val tvP = TextView(this)
            tvP.text = entry.stock.name + "\n" + entry.stock.code + " Qty:${entry.stock.qty}"
            tvP.textSize = 11f
            tvP.setTypeface(null, Typeface.BOLD)
            tvP.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)

            val tvOld = TextView(this)
            tvOld.text = "$${String.format("%.2f", entry.stock.selling)}"
            tvOld.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            val etNew = EditText(this)
            etNew.hint = "new"
            etNew.textSize = 12f
            etNew.setBackgroundColor(Color.parseColor("#FFFFFF"))
            etNew.setPadding(10,10,10,10)
            etNew.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if(entry.newPrice!=null) etNew.setText("${entry.newPrice}")

            val tvPerc = TextView(this)
            tvPerc.textSize = 11f
            tvPerc.setTypeface(null, Typeface.BOLD)
            tvPerc.setPadding(5,8,5,8)
            tvPerc.setBackgroundColor(Color.parseColor("#EEEEEE"))
            tvPerc.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)

            fun updatePerc(){
                try{
                val ns = etNew.text.toString().trim()
                if(ns.isEmpty()){
                    entry.newPrice=null
                    tvPerc.text=""
                    tvPerc.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    updateSummary()
                    return
                }
                val nv = ns.toDoubleOrNull() ?: return
                entry.newPrice=nv
                val old = entry.stock.selling
                if(old==0.0) return
                val perc = ((nv-old)/old)*100.0
                if(perc < 0){
                    tvPerc.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    tvPerc.setTextColor(Color.parseColor("#B71C1C"))
                    tvPerc.text = String.format("%.1f%% LOW", perc)
                }else if(perc <= 50.0){
                    tvPerc.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    tvPerc.setTextColor(Color.parseColor("#2E7D32"))
                    tvPerc.text = "+" + String.format("%.1f%% GOOD", perc)
                }else{
                    tvPerc.setBackgroundColor(Color.parseColor("#90CAF9"))
                    tvPerc.setTextColor(Color.parseColor("#0D47A1"))
                    tvPerc.text = "+" + String.format("%.1f%% HIGH", perc)
                }
                updateSummary()
                }catch(_:Exception){}
            }

            etNew.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){ updatePerc() }
                override fun afterTextChanged(s: Editable?){}
            })

            row.addView(tvP); row.addView(tvOld); row.addView(etNew); row.addView(tvPerc)
            container.addView(row)
        }
        tvCount.text = "${list.size} products"
        updateSummary()
        }catch(e:Exception){ Toast.makeText(this,"Render error",Toast.LENGTH_SHORT).show() }
    }

    private fun updateSummary(){
        try{
        var changed=0; var low=0; var good=0; var high=0
        for(e in allEntries){
            if(e.newPrice!=null){
                changed++
                val perc = ((e.newPrice!!-e.stock.selling)/e.stock.selling)*100.0
                if(perc<0) low++ else if(perc<=50) good++ else high++
            }
        }
        tvSummary.text="Changed:$changed | Low:$low | Good:$good | High:$high"
        }catch(_:Exception){}
    }

    private fun savePrices(){
        try{
        if(allEntries.none{it.newPrice!=null}){
            Toast.makeText(this,"Enter new price",Toast.LENGTH_SHORT).show()
            return
        }
        val gEdit = getSharedPreferences("goods_received", MODE_PRIVATE).edit()
        val sEdit = getSharedPreferences("stock_data", MODE_PRIVATE).edit()
        val pEdit = getSharedPreferences("products", MODE_PRIVATE).edit()
        val hPrefs = getSharedPreferences("price_history", MODE_PRIVATE)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var saved=0
        for(e in allEntries){
            if(e.newPrice!=null){
                val v = "${e.stock.code}|${e.stock.name}|${e.stock.qty}|${e.stock.cost}|${e.newPrice}"
                gEdit.putString(e.stock.code, v)
                sEdit.putString(e.stock.code, v)
                pEdit.putString(e.stock.code, v)
                hPrefs.edit().putString("${e.stock.code}_$date", "${e.stock.code}|${e.stock.name}|${e.stock.selling}|${e.newPrice}|$date").apply()
                saved++
            }
        }
        gEdit.apply(); sEdit.apply(); pEdit.apply()
        Toast.makeText(this,"Saved $saved prices",Toast.LENGTH_LONG).show()
        loadData()
        render(allEntries)
        }catch(e:Exception){ Toast.makeText(this,"Save error: ${e.message}",Toast.LENGTH_LONG).show() }
    }
}