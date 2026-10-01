package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class StockTransferActivity : Activity() {

    private lateinit var prefsShops: SharedPreferences
    private lateinit var prefsProducts: SharedPreferences
    private lateinit var prefsInventory: SharedPreferences
    private lateinit var prefsTransferLog: SharedPreferences

    private var fromShopId = ""
    private var toShopId = ""
    private var selectedProductKey = ""
    private var selectedProductName = ""
    private var selectedProductQty = 0f

    private lateinit var txtSelected: TextView
    private lateinit var txtLog: TextView
    private lateinit var edtQty: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefsShops = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
        prefsProducts = getSharedPreferences("products_db", Context.MODE_PRIVATE)
        prefsInventory = getSharedPreferences("stock_db", Context.MODE_PRIVATE)
        prefsTransferLog = getSharedPreferences("transfer_log", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10)}
        val header = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,16,12);gravity=Gravity.CENTER_VERTICAL}
        header.addView(TextView(this).apply{text="STOCK TRANSFER - BUILD 132 REAL MERGE";setTextColor(Color.WHITE);textSize=12f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        header.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(header)

        // SHOPS
        val shops = mutableListOf<Pair<String,String>>()
        prefsShops.all.forEach { (id, value) ->
            try{
                val parts = value.toString().split("|")
                val name = if(parts.size>=2) "${parts[0]} - ${parts[1]}" else id
                shops.add(Pair(id, name))
            }catch(_:Exception){ shops.add(Pair(id,id)) }
        }
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))

        fun shopNames(): List<String> = shops.map{it.second}
        fun shopIdFromName(name:String): String = shops.find{it.second==name}?.first?: "main"

        root.addView(TextView(this).apply{text="FROM SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        spinnerFrom = Spinner(this)
        spinnerFrom.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames())
        root.addView(spinnerFrom)

        root.addView(TextView(this).apply{text="TO SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        spinnerTo = Spinner(this)
        spinnerTo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames())
        root.addView(spinnerTo)

        root.addView(TextView(this).apply{text="SELECT PRODUCT FROM FROM SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        txtSelected = TextView(this).apply{text="No product selected";setPadding(12,12,12,12);setBackgroundColor(Color.parseColor("#F1F5F9"))}
        root.addView(txtSelected)

        val btnSelect = Button(this).apply{
            text="SELECT PRODUCT";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE)
            setOnClickListener{ showProductDialog() }
        }
        root.addView(btnSelect)

        root.addView(TextView(this).apply{text="QUANTITY TO TRANSFER *";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        edtQty = EditText(this).apply{hint="e.g. 5";setPadding(12,12,12,12)}
        root.addView(edtQty)

        val btnTransfer = Button(this).apply{
            text="TRANSFER NOW";setBackgroundColor(Color.parseColor("#22C55E"));setTextColor(Color.WHITE)
            setOnClickListener{ doTransfer() }
        }
        root.addView(btnTransfer)

        root.addView(TextView(this).apply{text="TRANSFER LOG";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        txtLog = TextView(this).apply{setPadding(12,12,12,12)}
        root.addView(txtLog)

        // Listeners to update shop ids
        fromShopId = shopIdFromName(spinnerFrom.selectedItem.toString())
        toShopId = shopIdFromName(spinnerTo.selectedItem.toString())

        refreshLog()
        setContentView(ScrollView(this).apply{addView(root)})
    }

    private fun getAllProducts(): List<Triple<String,String,Float>> {
        val list = mutableListOf<Triple<String,String,Float>>()
        // 1. FROM products_db — REAL — format: name|buy|sell|qty OR key is name
        prefsProducts.all.forEach { (key,value) ->
            try{
                val str = value.toString()
                val parts = str.split("|")
                if(parts.size>=4){
                    // parts: name|buy|sell|qty OR buy|sell|qty|name? handle both
                    val qty = parts[3].toFloatOrNull()?: parts.last().toFloatOrNull()?: 0f
                    val name = if(parts[0].length>2 && parts[0].any{it.isLetter()}) parts[0] else key
                    list.add(Triple(key, name, qty))
                } else if(parts.size==1){
                    // maybe value is qty only?
                    val qty = parts[0].toFloatOrNull()?: 0f
                    list.add(Triple(key, key, qty))
                } else {
                    // fallback
                    val qty = parts.last().toFloatOrNull()?: 0f
                    list.add(Triple(key, key, qty))
                }
            }catch(_:Exception){
                list.add(Triple(key, key, 0f))
            }
        }
        // 2. FROM stock_db — if exists (RECEIVE STOCK may use this)
        prefsInventory.all.forEach { (key,value) ->
            try{
                val str = value.toString()
                val parts = str.split("|")
                val qty = parts.last().toFloatOrNull()?: 0f
                val name = parts.firstOrNull()?: key
                if(list.none{it.second==name}) list.add(Triple(key, name, qty))
            }catch(_:Exception){}
        }
        // 3. ALSO check any other pref files that look like inventory
        listOf("inventory_db","stock","inventory").forEach { prefName ->
            try{
                val p = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                p.all.forEach { (key,value) ->
                    try{
                        val str = value.toString()
                        val qty = str.split("|").last().toFloatOrNull()?: str.toFloatOrNull()?: 0f
                        if(list.none{it.first==key}) list.add(Triple(key, key, qty))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        return list.filter{it.third>0 || true} // show all even if 0 for now
    }

    private fun showProductDialog(){
        val products = getAllProducts()
        if(products.isEmpty()){
            Toast.makeText(this,"No products found! First do RECEIVE STOCK",Toast.LENGTH_LONG).show()
            return
        }
        val names = products.map{"${it.second} (Qty:${it.third})"}.toTypedArray()
        AlertDialog.Builder(this)
           .setTitle("Select Product - REAL MERGE (${products.size} found)")
           .setItems(names){ _, which ->
                val p = products[which]
                selectedProductKey = p.first
                selectedProductName = p.second
                selectedProductQty = p.third
                txtSelected.text = "${p.second} | Stock: ${p.third} | Key:${p.first}"
            }.show()
    }

    private fun doTransfer(){
        if(selectedProductKey.isEmpty()){ Toast.makeText(this,"Select product first",Toast.LENGTH_SHORT).show(); return }
        val qtyStr = edtQty.text.toString()
        val qty = qtyStr.toFloatOrNull()
        if(qty==null || qty<=0){ Toast.makeText(this,"Enter valid qty",Toast.LENGTH_SHORT).show(); return }
        if(qty > selectedProductQty && selectedProductQty>0){ Toast.makeText(this,"Not enough stock! Have ${selectedProductQty}",Toast.LENGTH_LONG).show(); return }

        val fromName = spinnerFrom.selectedItem.toString()
        val toName = spinnerTo.selectedItem.toString()
        if(fromName==toName){ Toast.makeText(this,"FROM and TO cannot be same",Toast.LENGTH_SHORT).show(); return }

        // Update products_db qty
        try{
            val cur = prefsProducts.getString(selectedProductKey,"")
            if(cur!=null && cur.contains("|")){
                val parts = cur.split("|").toMutableList()
                if(parts.size>=4){
                    val oldQty = parts[3].toFloatOrNull()?: 0f
                    parts[3] = (oldQty - qty).toString()
                    prefsProducts.edit().putString(selectedProductKey, parts.joinToString("|")).apply()
                }
            }
            // Log
            val logEntry = "${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date())} | $qty x $selectedProductName FROM $fromName TO $toName"
            val oldLog = prefsTransferLog.getString("logs","")?: ""
            prefsTransferLog.edit().putString("logs", oldLog + "\n" + logEntry).apply()

            // Also save to audit
            val audit = getSharedPreferences("audit", Context.MODE_PRIVATE)
            val logs = audit.getStringSet("logs", mutableSetOf())?.toMutableSet()?: mutableSetOf()
            logs.add(logEntry)
            audit.edit().putStringSet("logs", logs).apply()

            Toast.makeText(this,"TRANSFERRED $qty x $selectedProductName SUCCESS",Toast.LENGTH_LONG).show()
            refreshLog()
            txtSelected.text="No product selected"
            selectedProductKey=""
            edtQty.setText("")
        }catch(e:Exception){
            Toast.makeText(this,"Transfer error: ${e.message}",Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshLog(){
        val logs = prefsTransferLog.getString("logs","")?: ""
        txtLog.text = if(logs.isBlank()) "No transfers yet" else logs.trim()
    }
}
