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
    private var selectedProductKey = ""
    private var selectedProductName = ""
    private var selectedProductQty = 0f
    private lateinit var txtSelected: TextView
    private lateinit var txtLog: TextView
    private lateinit var edtQty: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private val shops = mutableListOf<Pair<String,String>>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefsShops = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
        prefsProducts = getSharedPreferences("products_db", Context.MODE_PRIVATE)
        prefsInventory = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
        prefsTransferLog = getSharedPreferences("transfer_log", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10)}
        val header = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,16,12);gravity=Gravity.CENTER_VERTICAL}
        header.addView(TextView(this).apply{text="STOCK TRANSFER - BUILD 135 FILTERED REAL";setTextColor(Color.WHITE);textSize=12f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        header.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(header)

        prefsShops.all.forEach { (id,v) ->
            try{ val parts=v.toString().split("|"); val name=if(parts.size>=2) "${parts[0]} - ${parts[1]}" else id; shops.add(Pair(id,name)) }catch(_:Exception){ shops.add(Pair(id,id)) }
        }
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))

        fun shopNames(): List<String> = shops.map{it.second}

        root.addView(TextView(this).apply{text="FROM SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        spinnerFrom = Spinner(this); spinnerFrom.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames()); root.addView(spinnerFrom)
        root.addView(TextView(this).apply{text="TO SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        spinnerTo = Spinner(this); spinnerTo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames()); root.addView(spinnerTo)

        root.addView(TextView(this).apply{text="SELECT PRODUCT FROM FROM SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        txtSelected = TextView(this).apply{text="No product selected - Click SELECT";setPadding(12,12,12,12);setBackgroundColor(Color.parseColor("#F1F5F9"))}; root.addView(txtSelected)

        val btnSelect = Button(this).apply{
            text="SELECT PRODUCT";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE)
            setOnClickListener{ showProductDialog() }
        }; root.addView(btnSelect)

        root.addView(TextView(this).apply{text="QUANTITY TO TRANSFER *";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        edtQty = EditText(this).apply{hint="e.g. 5";setPadding(12,12,12,12)}; root.addView(edtQty)

        val btnTransfer = Button(this).apply{
            text="TRANSFER NOW";setBackgroundColor(Color.parseColor("#22C55E"));setTextColor(Color.WHITE)
            setOnClickListener{ doTransfer() }
        }; root.addView(btnTransfer)

        root.addView(TextView(this).apply{text="TRANSFER LOG";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        txtLog = TextView(this).apply{setPadding(12,12,12,12)}; root.addView(txtLog)

        refreshLog()
        setContentView(ScrollView(this).apply{addView(root)})
    }

    private fun getShopIdFromSpinner(spinner: Spinner): String {
        val pos = spinner.selectedItemPosition; return if(pos>=0 && pos<shops.size) shops[pos].first else "main"
    }

    private fun getProductsForFromShop(): List<Triple<String,String,Float>> {
        val fromId = getShopIdFromSpinner(spinnerFrom)
        val list = mutableListOf<Triple<String,String,Float>>()
        // 1. Check stock_fromId pref — REAL SHOP STOCK
        try{
            val shopPref = getSharedPreferences("stock_$fromId", Context.MODE_PRIVATE)
            shopPref.all.forEach { (k,v) ->
                try{ val parts=v.toString().split("|"); val qty=parts.getOrNull(3)?.toFloatOrNull()?:0f; val name=parts.getOrNull(0)?:k; if(qty>0) list.add(Triple(k,name,qty)) }catch(_:Exception){}
            }
        }catch(_:Exception){}
        // 2. Check inventory_db filtered by shopId
        prefsInventory.all.forEach { (k,v) ->
            try{
                val str=v.toString(); val parts=str.split("|"); if(parts.size>=5){ val shopIdIn=parts[4]; if(shopIdIn==fromId){ val qty=parts[3].toFloatOrNull()?:0f; val name=parts[0]; if(qty>0 && list.none{it.second==name}) list.add(Triple(k,name,qty)) } }
            }catch(_:Exception){}
        }
        // 3. If still empty — fallback to products_db GLOBAL (for old stock)
        if(list.isEmpty()){
            prefsProducts.all.forEach { (k,v) ->
                try{ val parts=v.toString().split("|"); if(parts.size>=4){ val qty=parts[3].toFloatOrNull()?:0f; val name=parts[0]; list.add(Triple(k,name,qty)) } }catch(_:Exception){}
            }
        }
        return list
    }

    private fun showProductDialog(){
        val products = getProductsForFromShop()
        if(products.isEmpty()){
            val fromId = getShopIdFromSpinner(spinnerFrom)
            Toast.makeText(this,"No stock in ${shops.find{it.first==fromId}?.second} — Do RECEIVE STOCK first to $fromId",Toast.LENGTH_LONG).show(); return
        }
        val names = products.map{"${it.second} (Available:${it.third})"}.toTypedArray()
        AlertDialog.Builder(this)
          .setTitle("Select from ${spinnerFrom.selectedItem} - ${products.size} items")
          .setItems(names){ _, which ->
                val p = products[which]; selectedProductKey=p.first; selectedProductName=p.second; selectedProductQty=p.third
                txtSelected.text="${p.second} | Available in ${spinnerFrom.selectedItem}: ${p.third} | Key:${p.first}"; txtSelected.setTextColor(Color.parseColor("#0F172A"))
            }.show()
    }

    private fun doTransfer(){
        if(selectedProductKey.isEmpty()){ Toast.makeText(this,"Select product first",Toast.LENGTH_SHORT).show(); return }
        val qty = edtQty.text.toString().toFloatOrNull(); if(qty==null||qty<=0){ Toast.makeText(this,"Enter valid qty",Toast.LENGTH_SHORT).show(); return }
        if(qty>selectedProductQty){ Toast.makeText(this,"Not enough stock! Have $selectedProductQty in ${spinnerFrom.selectedItem}",Toast.LENGTH_LONG).show(); return }
        val fromId = getShopIdFromSpinner(spinnerFrom); val toId = getShopIdFromSpinner(spinnerTo)
        val fromName = spinnerFrom.selectedItem.toString(); val toName = spinnerTo.selectedItem.toString()
        if(fromId==toId){ Toast.makeText(this,"FROM and TO cannot be same",Toast.LENGTH_SHORT).show(); return }
        try{
            // Deduct FROM shop
            val fromPref = getSharedPreferences("stock_$fromId", Context.MODE_PRIVATE)
            val curFrom = fromPref.getString(selectedProductKey,"")?: fromPref.getString(selectedProductName,"")?: ""
            if(curFrom.contains("|")){ val parts=curFrom.split("|").toMutableList(); val old=parts[3].toFloatOrNull()?:0f; parts[3]=(old-qty).toString(); fromPref.edit().putString(selectedProductKey, parts.joinToString("|")).apply(); if(fromPref.getString(selectedProductName,null)!=null) fromPref.edit().putString(selectedProductName, parts.joinToString("|")).apply() }
            // Deduct/Add inventory_db
            val invKeyFrom = "${fromId}_${selectedProductName}".replace(" ","_").lowercase()
            val invFrom = prefsInventory.getString(invKeyFrom,""); if(invFrom!=null&&invFrom.contains("|")){ val p=invFrom.split("|").toMutableList(); p[3]=(p[3].toFloatOrNull()?:0f - qty).toString(); if((p[3].toFloatOrNull()?:0f)<0) p[3]="0"; prefsInventory.edit().putString(invKeyFrom, p.joinToString("|")).apply() }
            val invKeyTo = "${toId}_${selectedProductName}".replace(" ","_").lowercase()
            val invTo = prefsInventory.getString(invKeyTo,""); var newToQty = qty; var buy="0"; var sell="0"; if(invTo!=null&&invTo.contains("|")){ val pt=invTo.split("|"); buy=pt.getOrNull(1)?:"0"; sell=pt.getOrNull(2)?:"0"; newToQty=(pt.getOrNull(3)?.toFloatOrNull()?:0f)+qty } else { // get from products_db
                val prod = prefsProducts.getString(selectedProductName,"")?: prefsProducts.getString(selectedProductKey,"")?:""; if(prod.contains("|")){ val pp=prod.split("|"); buy=pp.getOrNull(1)?:"0"; sell=pp.getOrNull(2)?:"0" }
            }
            prefsInventory.edit().putString(invKeyTo, "$selectedProductName|$buy|$sell|$newToQty|$toId").apply()
            // Add to TO shop pref
            val toPref = getSharedPreferences("stock_$toId", Context.MODE_PRIVATE)
            val curTo = toPref.getString(selectedProductName,"")?: toPref.getString(selectedProductKey,"")?: ""
            var toQty = qty; if(curTo.contains("|")){ toQty=(curTo.split("|").getOrNull(3)?.toFloatOrNull()?:0f)+qty; val pp=curTo.split("|").toMutableList(); pp[3]=toQty.toString(); toPref.edit().putString(selectedProductName, pp.joinToString("|")).apply() } else { toPref.edit().putString(selectedProductName, "$selectedProductName|$buy|$sell|$toQty|$toId").apply() }

            // Log
            val logEntry = "${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date())} | $qty x $selectedProductName FROM $fromId TO $toId"
            val oldLog = prefsTransferLog.getString("logs","")?:""; prefsTransferLog.edit().putString("logs", oldLog + "\n" + logEntry).apply()
            val audit = getSharedPreferences("audit", Context.MODE_PRIVATE); val logs = audit.getStringSet("logs", mutableSetOf())?.toMutableSet()?: mutableSetOf(); logs.add(logEntry); audit.edit().putStringSet("logs", logs).apply()

            Toast.makeText(this,"TRANSFERRED $qty x $selectedProductName FROM $fromName TO $toName",Toast.LENGTH_LONG).show()
            refreshLog(); txtSelected.text="No product selected - Click SELECT"; selectedProductKey=""; edtQty.setText("")
        }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun refreshLog(){ val logs=prefsTransferLog.getString("logs","")?:""; txtLog.text=if(logs.isBlank()) "No transfers yet" else logs.trim() }
}
