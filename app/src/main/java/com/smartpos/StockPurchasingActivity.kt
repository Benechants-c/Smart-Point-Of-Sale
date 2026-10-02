package com.smartpos

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject

data class RealProd(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int, val shopId: String)

class StockPurchasingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var tvHeader: TextView
    private lateinit var spinnerShop: Spinner
    private lateinit var etSearch: EditText
    private var fullList: List<RealProd> = listOf()
    private var filtered: List<RealProd> = listOf()
    private val shops = mutableListOf<Pair<String,String>>()

    private fun getSelectedShopIdSafe(): String {
        return try {
            if(!::spinnerShop.isInitialized) return "main"
            val pos = spinnerShop.selectedItemPosition
            if(pos in shops.indices) shops[pos].first else "main"
        } catch(_:Exception){ "main" }
    }

    private fun loadShops(){
        shops.clear()
        try{
            val prefs = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            prefs.all.forEach { (keyId, value) ->
                try{
                    val str = value.toString()
                    if(str.startsWith("{")){
                        val json = JSONObject(str)
                        val id = json.optString("id", keyId)
                        val name = json.optString("name", id)
                        shops.add(Pair(id, "$name ($id)"))
                    } else { shops.add(Pair(keyId, keyId)) }
                }catch(_:Exception){ shops.add(Pair(keyId, keyId)) }
            }
        }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
    }

    private fun loadRealStock(shopId: String): List<RealProd>{
        val map = mutableMapOf<String, RealProd>()
        try{
            val inv = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
            inv.all.forEach { (k,v) ->
                try{
                    if(!k.lowercase().startsWith("${shopId.lowercase()}_")) return@forEach
                    val parts = v.toString().split("|")
                    if(parts.size < 4) return@forEach
                    val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                    val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                    val code = k.substringAfter("_","")
                    map[code.lowercase()] = RealProd(name, code, cost, sell, qty, shopId)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        try{
            val pref = getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            pref.all.forEach { (code, v) ->
                try{
                    val parts = v.toString().split("|")
                    val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                    val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                    if(!map.containsKey(code.lowercase()) || qty > (map[code.lowercase()]?.qty?:0))
                        map[code.lowercase()] = RealProd(name, code, cost, sell, qty, shopId)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return map.values.sortedBy { it.name.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_purchasing)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Stock & Purchasing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { showTools() }

        tvHeader = findViewById(R.id.tvHeader)
        recyclerView = findViewById(R.id.recyclerView)
        spinnerShop = findViewById(R.id.spinnerShop)
        etSearch = findViewById(R.id.etSearch)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadShops()
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.map{it.second})
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long){ reload() }
            override fun onNothingSelected(p0: AdapterView<*>?){}
        }
        etSearch.addTextChangedListener(object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        reload()
    }

    private fun reload(){
        val shopId = getSelectedShopIdSafe()
        fullList = loadRealStock(shopId)
        filter(etSearch.text.toString())
    }

    private fun filter(q: String){
        filtered = if(q.trim().isEmpty()) fullList else fullList.filter { it.name.contains(q, true) || it.code.contains(q, true) }
        if(!::adapter.isInitialized){
            adapter = StockAdapter(filtered)
            recyclerView.adapter = adapter
        } else adapter.update(filtered)
        var total = 0.0; var qtySum = 0
        for(p in filtered){ total += p.qty * p.cost; qtySum += p.qty }
        val shopName = try{ shops.find{it.first==getSelectedShopIdSafe()}?.second?: getSelectedShopIdSafe() } catch(_:Exception){ getSelectedShopIdSafe() }
        tvHeader.text = "Products ${filtered.size}/${fullList.size} | Qty:$qtySum | Value:$${"%.2f".format(total)} | $shopName"
    }

    private fun showTools(){
        val v = layoutInflater.inflate(R.layout.bottom_sheet_stock_tools, null)
        val d = BottomSheetDialog(this); d.setContentView(v)
        v.findViewById<View>(R.id.btnPrint).setOnClickListener { d.dismiss()
            var txt = "STOCK ${getSelectedShopIdSafe()}\n"; for(p in filtered) txt += "${p.name}|${p.code}|Qty:${p.qty}\n"
            val i = android.content.Intent(android.content.Intent.ACTION_SEND); i.type="text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT, txt); startActivity(android.content.Intent.createChooser(i,"PRINT"))
        }
        d.show()
    }

    inner class StockAdapter(private var list: List<RealProd>) : RecyclerView.Adapter<StockAdapter.VH>(){
        inner class VH(view: View): RecyclerView.ViewHolder(view){
            val root = view.findViewById<View>(R.id.rowRoot)
            val name = view.findViewById<TextView>(R.id.tvProductName)
            val qty = view.findViewById<TextView>(R.id.tvQty)
            val buy = view.findViewById<TextView>(R.id.tvBuy)
            val sell = view.findViewById<TextView>(R.id.tvSell)
            val profit = view.findViewById<TextView>(R.id.tvProfit)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_stock_row, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = list[pos]
            h.name.text = "${p.name} (${p.code})"
            h.qty.text = "${p.qty}"; h.buy.text = "$${p.cost}"; h.sell.text = "$${p.sell}"
            h.profit.text = "$${"%.2f".format(p.sell - p.cost)}"
            h.root.setBackgroundColor(if(pos%2==0) Color.WHITE else Color.parseColor("#F1F5F9"))
            if(p.qty <=5) h.qty.setTextColor(Color.RED) else h.qty.setTextColor(Color.parseColor("#0F172A"))
        }
        fun update(newList: List<RealProd>){ list = newList; notifyDataSetChanged() }
    }
}
