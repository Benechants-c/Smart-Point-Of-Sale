package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class ReceiveStockActivity : Activity() {

    private lateinit var prefsShops: SharedPreferences
    private lateinit var prefsProducts: SharedPreferences
    private lateinit var prefsInventory: SharedPreferences
    private lateinit var spinnerShop: Spinner

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefsShops = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
        prefsProducts = getSharedPreferences("products_db", Context.MODE_PRIVATE)
        prefsInventory = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(12,12,12,12)}
        val header = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,16,12);gravity=Gravity.CENTER_VERTICAL}
        header.addView(TextView(this).apply{text="RECEIVE STOCK - BUILD 133 SHOP PICKER REAL";setTextColor(Color.WHITE);textSize=12f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        header.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(header)

        // LOAD SHOPS — REAL FROM shops_db
        val shops = mutableListOf<Pair<String,String>>()
        prefsShops.all.forEach { (id, v) ->
            try{
                val parts = v.toString().split("|")
                val name = if(parts.size>=1) "${parts[0]} - ${if(parts.size>1) parts[1] else ""}" else id
                shops.add(Pair(id, name))
            }catch(_:Exception){ shops.add(Pair(id,id)) }
        }
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))

        root.addView(TextView(this).apply{text="SELECT SHOP TO RECEIVE *";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        spinnerShop = Spinner(this)
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.map{it.second})
        root.addView(spinnerShop)

        root.addView(TextView(this).apply{text="PRODUCT NAME *";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        val edtName = EditText(this).apply{hint="e.g. Bread, Sugar";setPadding(12,12,12,12)}
        root.addView(edtName)

        root.addView(TextView(this).apply{text="BUY PRICE";setBackgroundColor(Color.parseColor("#334155"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=10f})
        val edtBuy = EditText(this).apply{hint="e.g. 1.50";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setPadding(12,12,12,12)}
        root.addView(edtBuy)

        root.addView(TextView(this).apply{text="SELL PRICE";setBackgroundColor(Color.parseColor("#334155"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=10f})
        val edtSell = EditText(this).apply{hint="e.g. 2.00";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setPadding(12,12,12,12)}
        root.addView(edtSell)

        root.addView(TextView(this).apply{text="QUANTITY *";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=11f;setTypeface(null,Typeface.BOLD)})
        val edtQty = EditText(this).apply{hint="e.g. 50";inputType=android.text.InputType.TYPE_CLASS_NUMBER;setPadding(12,12,12,12)}
        root.addView(edtQty)

        root.addView(TextView(this).apply{text="SUPPLIER (optional)";setBackgroundColor(Color.parseColor("#334155"));setTextColor(Color.WHITE);setPadding(8,6,8,6);textSize=10f})
        val edtSupplier = EditText(this).apply{hint="e.g. Chiedza Supplier";setPadding(12,12,12,12)}
        root.addView(edtSupplier)

        val txtInfo = TextView(this).apply{text="Ready to receive";setPadding(12,12,12,12);setTextColor(Color.parseColor("#64748B"));textSize=11f}
        root.addView(txtInfo)

        val btnReceive = Button(this).apply{
            text="RECEIVE STOCK NOW";setBackgroundColor(Color.parseColor("#22C55E"));setTextColor(Color.WHITE);textSize=14f
            setOnClickListener{
                val shopPos = spinnerShop.selectedItemPosition
                val shopId = shops[shopPos].first
                val shopName = shops[shopPos].second
                val name = edtName.text.toString().trim()
                val buy = edtBuy.text.toString().trim().ifEmpty{"0"}
                val sell = edtSell.text.toString().trim().ifEmpty{"0"}
                val qtyStr = edtQty.text.toString().trim()
                val supplier = edtSupplier.text.toString().trim()

                if(name.isEmpty() || qtyStr.isEmpty()){ Toast.makeText(this@ReceiveStockActivity,"Name & Qty required",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val qty = qtyStr.toFloatOrNull()?: 0f
                if(qty<=0){ Toast.makeText(this@ReceiveStockActivity,"Invalid qty",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                try{
                    // 1. UPDATE products_db — GLOBAL — name|buy|sell|qty
                    val existing = prefsProducts.getString(name,"")
                    var newQty = qty
                    var finalBuy = buy
                    var finalSell = sell
                    if(existing!=null && existing.contains("|")){
                        val p = existing.split("|")
                        if(p.size>=4){
                            finalBuy = if(buy=="0") p[1] else buy
                            finalSell = if(sell=="0") p[2] else sell
                            newQty = (p[3].toFloatOrNull()?:0f) + qty
                        }
                    }
                    val prodValue = "$name|$finalBuy|$finalSell|$newQty"
                    prefsProducts.edit().putString(name, prodValue).apply()

                    // 2. UPDATE inventory_db — PER SHOP — shopId_name => name|buy|sell|qty|shopId
                    val invKey = "${shopId}_${name}".replace(" ","_").lowercase()
                    val existingInv = prefsInventory.getString(invKey,"")
                    var invQty = qty
                    if(existingInv!=null && existingInv.contains("|")){
                        val ip = existingInv.split("|")
                        invQty = (ip.getOrNull(3)?.toFloatOrNull()?:0f) + qty
                    }
                    val invValue = "$name|$finalBuy|$finalSell|$invQty|$shopId"
                    prefsInventory.edit().putString(invKey, invValue).apply()

                    // 3. ALSO save to shop specific pref — stock_shopId
                    val shopPref = getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
                    val shopExisting = shopPref.getString(name,"")
                    var shopQty = qty
                    if(shopExisting!=null && shopExisting.contains("|")){
                        shopQty = (shopExisting.split("|").getOrNull(3)?.toFloatOrNull()?:0f) + qty
                    }
                    shopPref.edit().putString(name, "$name|$finalBuy|$finalSell|$shopQty|$shopId").apply()

                    // 4. Audit log
                    val audit = getSharedPreferences("audit", Context.MODE_PRIVATE)
                    val logs = audit.getStringSet("logs", mutableSetOf())?.toMutableSet()?: mutableSetOf()
                    val log = "${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date())} RECEIVE $qty x $name to $shopName by ${if(supplier.isEmpty()) "GRN" else supplier}"
                    logs.add(log)
                    audit.edit().putStringSet("logs", logs).apply()

                    txtInfo.text = "✅ RECEIVED $qty x $name to $shopName\nNew Global Qty: $newQty\nShop Qty: $invQty"
                    txtInfo.setTextColor(Color.parseColor("#16A34A"))
                    txtInfo.setTypeface(null, Typeface.BOLD)
                    Toast.makeText(this@ReceiveStockActivity,"✅ Stock added to $shopName",Toast.LENGTH_LONG).show()

                    edtName.setText(""); edtQty.setText(""); edtSupplier.setText("")

                }catch(e:Exception){
                    Toast.makeText(this@ReceiveStockActivity,"Error: ${e.message}",Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(btnReceive)

        setContentView(ScrollView(this).apply{addView(root)})
    }
}
