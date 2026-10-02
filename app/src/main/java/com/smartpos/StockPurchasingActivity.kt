package com.smartpos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog

class StockPurchasingActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var tvHeader: TextView
    private var fullList: List<Product> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_purchasing)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Stock & Purchasing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        dbHelper = DatabaseHelper(this)
        tvHeader = findViewById(R.id.tvHeader)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadData()
        toolbar.setNavigationOnClickListener { showTools() }
        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if(q.isEmpty()) fullList else fullList.filter { it.name.contains(q, true) }
                adapter.update(filtered)
                updateHeader(filtered)
            }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
    }
    private fun loadData(){
        fullList = dbHelper.getAllProducts()
        adapter = StockAdapter(fullList)
        recyclerView.adapter = adapter
        updateHeader(fullList)
    }
    private fun updateHeader(list: List<Product>){
        val totalValue = list.sumOf { it.qty * it.buyPrice }
        tvHeader.text = "Products - ${list.size} Items | Value: $${"%.2f".format(totalValue)}"
    }
    private fun showTools(){
        val v = layoutInflater.inflate(R.layout.bottom_sheet_stock_tools, null)
        val d = BottomSheetDialog(this)
        d.setContentView(v)
        v.findViewById<View>(R.id.btnAdjustments).setOnClickListener { d.dismiss(); startActivity(Intent(this, StockAdjustmentActivity::class.java)) }
        v.findViewById<View>(R.id.btnHistory).setOnClickListener { d.dismiss(); startActivity(Intent(this, AuditLogActivity::class.java)) }
        v.findViewById<View>(R.id.btnPrint).setOnClickListener { d.dismiss(); startActivity(Intent(this, ReportsActivity::class.java).apply{putExtra("type","stock_sheet")}) }
        v.findViewById<View>(R.id.btnHelp).setOnClickListener { d.dismiss(); Toast.makeText(this,"RED=Low Stock Bold, GREEN=Profit Bold, Zebra=Eye comfort",Toast.LENGTH_LONG).show() }
        d.show()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_stock_purchasing, menu)
        return true
    }
    inner class StockAdapter(private var list: List<Product>) : RecyclerView.Adapter<StockAdapter.VH>(){
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
            h.name.text = p.name
            h.qty.text = "${p.qty}"
            h.buy.text = "$${p.buyPrice}"
            h.sell.text = "$${p.sellPrice}"
            val prof = p.sellPrice - p.buyPrice
            h.profit.text = "$${"%.2f".format(prof)}"
            h.root.setBackgroundColor(if(pos % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F1F5F9"))
            if(p.qty <= 5){ h.qty.setTextColor(Color.parseColor("#DC2626")); h.qty.text = "${p.qty} LOW!" } else { h.qty.setTextColor(Color.parseColor("#0F172A")) }
            h.profit.setTextColor(if(prof > 0) Color.parseColor("#16A34A") else Color.RED)
        }
        fun update(newList: List<Product>){ list = newList; notifyDataSetChanged() }
    }
}
