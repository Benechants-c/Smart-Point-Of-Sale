package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*

class StockPurchasingActivity : Activity() {

    data class Prod(val name:String, val qty:Float, val price:Float)

    private var allProducts = ArrayList<Prod>()
    private lateinit var listContainer: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var searchBox: EditText
    private lateinit var shopSpinner: Spinner
    private var currentShop = "Main Shop"

    // === YOUR REAL ADMIN PASSWORDS ===
    private val MASTER_PASSWORDS = listOf("admin123", "1234", "2026", "admin")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F1F5F9"))
            }

            // Top Bar
            val topBar = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(30,50,30,30)
                orientation = LinearLayout.HORIZONTAL
            }
            val back = TextView(this).apply {
                text=" <- Back"; textSize=14f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD)
                setOnClickListener{ finish() }
            }
            val title = TextView(this).apply {
                text=" Stock & Purchasing"; textSize=16f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE)
            }
            topBar.addView(back); topBar.addView(title)
            root.addView(topBar)

            // === ACTION BUTTONS BAR - POLISHED ===
            val actionBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12,12,12,12)
                setBackgroundColor(Color.WHITE)
                elevation = 4f
            }

            val btnReceive = Button(this).apply {
                text = "📥 RECEIVE"
                textSize = 11f
                setBackgroundColor(Color.parseColor("#2563EB"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,6,0) }
                setOnClickListener { startActivity(Intent(this@StockPurchasingActivity, ReceiveStockActivity::class.java)) }
            }
            val btnTransfer = Button(this).apply {
                text = "🔄 TRANSFER"
                textSize = 11f
                setBackgroundColor(Color.parseColor("#0D9488"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,12,0) }
                setOnClickListener { startActivity(Intent(this@StockPurchasingActivity, StockTransferActivity::class.java)) }
            }
            // SEPARATE + CRITICAL BUTTON WITH DIVIDER
            val divider = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(2, -1).apply{ setMargins(0,0,12,0) }
            }
            val btnStockTake = Button(this).apply {
                text = "🔐 STOCK TAKE"
                textSize = 11f
                setBackgroundColor(Color.parseColor("#7C3AED"))
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0,-2,1.4f)
                setOnClickListener { askPasswordForStockTake() }
            }
            actionBar.addView(btnReceive)
            actionBar.addView(btnTransfer)
            actionBar.addView(divider)
            actionBar.addView(btnStockTake)
            root.addView(actionBar)

            val shops = arrayOf("Main Shop","Branch 1","Warehouse")
            shopSpinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shops)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            shopSpinner.adapter = adapter
            val shopWrap = LinearLayout(this).apply {
                setPadding(16,10,16,10); setBackgroundColor(Color.WHITE)
                addView(TextView(this@StockPurchasingActivity).apply{ text="Shop: "; textSize=12f; setTypeface(null,Typeface.BOLD) })
                addView(shopSpinner)
            }
            root.addView(shopWrap)

            searchBox = EditText(this).apply {
                hint = "🔍 Search name or code..."
                setPadding(24,20,24,20)
                setBackgroundColor(Color.WHITE)
                background = getDrawable(android.R.drawable.editbox_background)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(16,8,16,8) }
            }
            root.addView(searchBox)

            headerTitle = TextView(this).apply {
                text = "Products 0 | Value: 0 | Main Shop"
                setPadding(16,12,16,12)
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                setTypeface(null,Typeface.BOLD)
                textSize = 12f
            }
            root.addView(headerTitle)

            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,12,16,12)
                elevation = 2f
            }
            headerRow.addView(TextView(this).apply { text="PRODUCT"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#64748B")); layoutParams = LinearLayout.LayoutParams(0,-2,2.5f) })
            headerRow.addView(TextView(this).apply { text="QTY"; textSize=11f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setTextColor(Color.parseColor("#64748B")); layoutParams = LinearLayout.LayoutParams(0,-2,0.7f) })
            headerRow.addView(TextView(this).apply { text="PRICE"; textSize=11f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setTextColor(Color.parseColor("#64748B")); layoutParams = LinearLayout.LayoutParams(0,-2,0.9f) })
            root.addView(headerRow)

            val scroll = ScrollView(this)
            listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
            scroll.addView(listContainer)
            // Weight to fill remaining space
            root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

            setContentView(root)

            loadProducts()
            render()

            shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(a:AdapterView<*>?, v:android.view.View?, p:Int, id:Long){
                    currentShop = shops[p]
                    render()
                }
                override fun onNothingSelected(a:AdapterView<*>?){}
            }

            searchBox.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s:Editable?){ render() }
                override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
                override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            })

        } catch(e:Exception){
            val tv = TextView(this); tv.text="Error: "+e.message; setContentView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh when coming back from Receive/Transfer
        try { loadProducts(); render() } catch(_:Exception){}
    }

    // === PASSWORD PROTECTION - SEPARATE CRITICAL ACTIVITY ===
    private fun askPasswordForStockTake(){
        val input = EditText(this).apply {
            hint = "Enter Manager / Admin Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(50,40,50,40)
            textSize = 15f
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30,20,30,10)
            addView(input)
            addView(TextView(this@StockPurchasingActivity).apply {
                text = "Default: 1234 or admin123"
                textSize = 11f
                setTextColor(Color.GRAY)
                setPadding(0,10,0,0)
            })
        }

        AlertDialog.Builder(this)
          .setTitle("🔐 Stock Take - Protected")
          .setMessage("This is a critical activity that will adjust stock levels. Manager authorization required.")
          .setView(container)
          .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text.toString().trim()
                val prefs = getSharedPreferences("smartpos_prefs", Context.MODE_PRIVATE)
                val adminPass = prefs.getString("admin_password", "admin123")?: "admin123"
                val managerPin = prefs.getString("manager_pin", "1234")?: "1234"

                if (entered == adminPass || entered == managerPin || MASTER_PASSWORDS.contains(entered)) {
                    Toast.makeText(this, "✅ Access Granted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, StockTakeActivity::class.java))
                } else {
                    Toast.makeText(this, "❌ Wrong password! Access Denied", Toast.LENGTH_LONG).show()
                }
            }
          .setNegativeButton("Cancel", null)
          .show()
    }

    private fun loadProducts(){
        allProducts.clear()
        try{
            val seen = HashSet<String>()
            val files = listOf("stock_main","products_db","products")
            for(fname in files){
                val pref = getSharedPreferences(fname, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    if(seen.contains(k)) continue
                    seen.add(k)
                    try{
                        val parts = v.toString().split("|")
                        val name = if(parts.isNotEmpty()) parts[0] else k
                        var sell = if(parts.size>2) parts[2].toFloatOrNull()?:0f else 0f
                        var qty = 1f
                        if(parts.size>3) qty = parts[3].toFloatOrNull()?:1f
                        if(name.isNotBlank()) allProducts.add(Prod(name,qty,sell))
                    }catch(_:Exception){}
                }
            }
        }catch(_:Exception){}
    }

    private fun render(){
        try{
            listContainer.removeAllViews()
            val filter = searchBox.text.toString().lowercase()
            val filtered = if(filter.isBlank()) allProducts else allProducts.filter{ it.name.lowercase().contains(filter) }

            var totalQty = 0f
            var totalValue = 0f
            for(p in filtered){ totalQty+=p.qty; totalValue+=p.qty*p.price }

            headerTitle.text = "Products ${filtered.size}/${allProducts.size} | Qty:${totalQty.toInt()} | Value:$${String.format("%.2f",totalValue)} | $currentShop"

            for(prod in filtered){
                val isCritical = prod.qty <= 5f
                val isLow = prod.qty <= 10f

                val color = when {
                    isCritical -> Color.parseColor("#DC2626") // Dark Red <=5
                    isLow -> Color.parseColor("#EA580C") // Orange 6-10
                    else -> Color.parseColor("#0F172A") // Black >10
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16,18,16,18)
                }
                // Divider line
                val dividerLine = View(this).apply {
                    setBackgroundColor(Color.parseColor("#F1F5F9"))
                    layoutParams = LinearLayout.LayoutParams(-1,2)
                }

                row.addView(TextView(this@StockPurchasingActivity).apply {
                    text = prod.name
                    textSize = 13f
                    setTextColor(color)
                    if(isCritical) setTypeface(null,Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0,-2,2.5f)
                })
                row.addView(TextView(this@StockPurchasingActivity).apply {
                    text = prod.qty.toInt().toString()
                    textSize=13f
                    setTypeface(null,Typeface.BOLD)
                    setTextColor(color)
                    gravity=Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0,-2,0.7f)
                })
                row.addView(TextView(this@StockPurchasingActivity).apply {
                    text = "$"+String.format("%.2f",prod.price)
                    textSize=13f
                    setTextColor(color)
                    gravity=Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0,-2,0.9f)
                })
                listContainer.addView(row)
                listContainer.addView(dividerLine)
            }
        }catch(_:Exception){}
    }
}