package com.smartpos

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private var productList: MutableList<Product> = mutableListOf()
    private lateinit var tvHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_purchasing)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Stock & Purchasing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu) // ☰

        dbHelper = DatabaseHelper(this)
        tvHeader = findViewById(R.id.tvHeader)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadRealData() // YOUR DATA FROM ReceiveStock

        // ☰ click -> bottom sheet with 4 tools only
        toolbar.setNavigationOnClickListener { showTools() }

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?) {
                val filtered = productList.filter { it.name.contains(s.toString(), true) }
                adapter.updateList(filtered)
                updateHeader(filtered)
            }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
    }

    private fun loadRealData(){
        productList = dbHelper.getAllProducts().toMutableList() // SAME TABLE AS ReceiveStockActivity USES!
        adapter = StockAdapter(productList)
        recyclerView.adapter = adapter
        updateHeader(productList)
    }

    private fun updateHeader(list: List<Product>){
        val total = list.sumOf { it.qty * it.buyPrice }
        tvHeader.text = "Products - ${list.size} Items | Value: $${String.format("%.2f", total)}"
    }

    private fun showTools(){
        val view = layoutInflater.inflate(R.layout.bottom_sheet_stock_tools, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)
        view.findViewById<View>(R.id.btnAdjustments).setOnClickListener {
            dialog.dismiss(); startActivity(Intent(this, StockAdjustmentActivity::class.java))
        }
        view.findViewById<View>(R.id.btnHistory).setOnClickListener {
            dialog.dismiss(); startActivity(Intent(this, AuditLogActivity::class.java))
        }
        view.findViewById<View>(R.id.btnPrint).setOnClickListener {
            dialog.dismiss(); startActivity(Intent(this, ReportsActivity::class.java).apply { putExtra("type","stock_sheet") })
        }
        view.findViewById<View>(R.id.btnHelp).setOnClickListener {
            dialog.dismiss(); Toast.makeText(this, "RED qty = low stock (bold), GREEN = profit margin (bold), Zebra = easy reading. Adjustments edit qty/price.", Toast.LENGTH_LONG).show()
        }
        dialog.show()
    }

    // KEEP YOUR EXISTING MENU - THIS WILL NOT BREAK ReceiveStock
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_stock_purchasing, menu)
        return true
    }
}
