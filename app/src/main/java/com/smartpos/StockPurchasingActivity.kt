package com.smartpos

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog

data class SimpleProduct(val name: String, val qty: Int, val buy: Double, val sell: Double)

class StockPurchasingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var tvHeader: TextView
    private var fullList: List<SimpleProduct> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_purchasing)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Stock & Purchasing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        tvHeader = findViewById(R.id.tvHeader)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fullList = listOf(
            SimpleProduct("Coke 500ml", 12, 0.50, 0.80),
            SimpleProduct("Bread", 3, 0.60, 1.00),
            SimpleProduct("Milk", 20, 0.90, 1.20)
        )
        adapter = StockAdapter(fullList)
        recyclerView.adapter = adapter
        var total = 0.0
        for(p in fullList) total += p.qty * p.buy
        tvHeader.text = "Products - ${fullList.size} Items | Value: $${"%.2f".format(total)}"
        toolbar.setNavigationOnClickListener {
            val v = layoutInflater.inflate(R.layout.bottom_sheet_stock_tools, null)
            val d = BottomSheetDialog(this)
            d.setContentView(v)
            d.show()
        }
    }
    inner class StockAdapter(private var list: List<SimpleProduct>) : RecyclerView.Adapter<StockAdapter.VH>(){
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
            h.buy.text = "$${p.buy}"
            h.sell.text = "$${p.sell}"
            val prof = p.sell - p.buy
            h.profit.text = "$${"%.2f".format(prof)}"
            h.root.setBackgroundColor(if(pos % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F1F5F9"))
        }
        fun update(newList: List<SimpleProduct>){ list = newList; notifyDataSetChanged() }
    }
}
