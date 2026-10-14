package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val cost: Double, val sell: Double, var qty: Int, val shopId: String, val dept: String)
    data class CartItem(var product: Product, var qty: Int)

    private val allProducts = mutableListOf<Product>()
    private val filtered = mutableListOf<Product>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var spinnerShop: Spinner
    private lateinit var cartLayout: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var receiptView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private var cashierName = "Cashier"
    private var lastReceiptText = ""

    private fun getCurrency(): String = getSharedPreferences("system_settings",0).getString("currency","$")?:"$"
    private fun getShopNameSet(): String = getSharedPreferences("system_settings",0).getString("shop_name","Main Shop")?:"Main Shop"
    private fun getShopAddrSet(): String = getSharedPreferences("system_settings",0).getString("address","")?:""
    private fun getShopPhoneSet(): String = getSharedPreferences("system_settings",0).getString("phone","")?:""
    private fun getReceiptHeaderSet(): String = getSharedPreferences("receipt_settings",0).getString("header","*** THANK YOU ***")?:"*** THANK YOU ***"
    private fun getReceiptFooterSet(): String = getSharedPreferences("receipt_settings",0).getString("footer","Come Again!")?:"Come Again!"

    private fun getShopId(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getTotal(): Double { var t=0.0; for(c in cart) t+=c.product.sell*c.qty; return t }
    private fun getProfit(): Double { var p=0.0; for(c in cart) p+=(c.product.sell-c.product.cost)*c.qty; return p }
    private fun calcChange(){
        val cur=getCurrency(); val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0; val change=tendered-total
        if(tendered==0.0) changeView.text="CHANGE: ${cur}0.00"
        else if(change<0) changeView.text="NEED ${cur}${String.format(Locale.getDefault(),"%.2f",-change)}"
        else changeView.text="CHANGE: ${cur}${String.format(Locale.getDefault(),"%.2f",change)}"
    }

    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val pref=getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            for((code,v) in pref.all){ try{ val p=v.toString().split("|"); if(p.size>=4){ val name=p[0]; val cost=p[1].toDoubleOrNull()?:0.0; val sell=p[2].toDoubleOrNull()?:0.0; val qty=p[3].toIntOrNull()?:0; allProducts.add(Product(code,name,cost,sell,qty,shopId,"General")) } }catch(_:Exception){} }
        }catch(_:Exception){}
        if(allProducts.isEmpty()){
            try{ val pref=getSharedPreferences("stock_main",0); for((code,v) in pref.all){ val p=v.toString().split("|"); if(p.size>=4){ allProducts.add(Product(code,p[0],p[1].toDoubleOrNull()?:0.0,p[2].toDoubleOrNull()?:0.0,p[3].toIntOrNull()?:0,"main","General")) } } }catch(_:Exception){}
        }
        refreshCart()
    }

    private fun showSearchDialog(){
        val dlg=Dialog(this); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE)}
        root.addView(TextView(this).apply{text="Search Product"; textSize=16f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(16,12,16,12)})
        val ed=EditText(this).apply{hint="Enter name or code..."; setPadding(20,14,20,14); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        root.addView(ed); val listLay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val scroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(listLay)}; root.addView(scroll)
        fun refreshList(q:String){
            listLay.removeAllViews(); filtered.clear(); val qq=q.trim().lowercase()
            for(p in allProducts){ if(qq.isEmpty()||p.name.lowercase().contains(qq)||p.code.lowercase().contains(qq)) filtered.add(p) }
            for(p in filtered){
                val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(14,12,14,12); setBackgroundColor(Color.WHITE)}
                val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                left.addView(TextView(this).apply{text=p.name; textSize=16f; setTypeface(null,Typeface.BOLD)})
                left.addView(TextView(this).apply{text="${p.code} | Stock:${p.qty}"; textSize=11f; setTextColor(Color.GRAY)})
                val cur=getCurrency()
                val price=TextView(this).apply{text="$cur${p.sell}"; textSize=14f; setTypeface(null,Typeface.BOLD); gravity=Gravity.END}
                row.addView(left); row.addView(price); row.setOnClickListener{ addToCart(p); dlg.dismiss() }; listLay.addView(row)
            }
        }
        ed.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){refreshList(s.toString())} override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
        refreshList(""); val close=Button(this).apply{text="CLOSE"}; close.setOnClickListener{dlg.dismiss()}; root.addView(close)
        dlg.setContentView(root); dlg.show()
    }

    private fun addToCart(p: Product){ val ex=cart.find{it.product.code==p.code}; if(ex!=null){ if(ex.qty<p.qty) ex.qty++ } else cart.add(CartItem(p,1)); refreshCart() }

    private fun refreshCart(){
        cartLayout.removeAllViews(); val cur=getCurrency(); var total=0.0
        for(i in cart.indices){ val curr=cart[i]; val line=curr.product.sell*curr.qty; total+=line
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,12,10,12); setBackgroundColor(Color.WHITE)}
            row.addView(TextView(this).apply{text="${i+1}. ${curr.product.name}"; layoutParams=LinearLayout.LayoutParams(0,-2,2f); textSize=14f; setTypeface(null,Typeface.BOLD)})
            row.addView(TextView(this).apply{text="${curr.qty}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#E2E8F0"))})
            row.addView(TextView(this).apply{text="$cur${String.format(Locale.getDefault(),"%.2f",line)}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.END; setTypeface(null,Typeface.BOLD)})
            cartLayout.addView(row)
        }
        receiptView.text="RECEIPT: ${cart.size} items"; totalView.text="TOTAL: $cur${String.format(Locale.getDefault(),"%.2f",total)}"; calcChange()
    }

    private fun completeSale(){
        if(cart.isEmpty()){Toast.makeText(this,"Cart empty",0).show(); return}
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0
        if(tendered<total){Toast.makeText(this,"Tendered less",1).show(); return}
        try{
            val shopId=getShopId()
            for(item in cart){ val pref=getSharedPreferences("stock_$shopId",0); val old=pref.getString(item.product.code,"")?:""; if(old.contains("|")){ val parts=old.split("|").toMutableList(); val oq=parts.getOrNull(3)?.toIntOrNull()?:0; parts[3]=(oq-item.qty).toString(); pref.edit().putString(item.product.code,parts.joinToString("|")).apply() } }
            val key="SALE_${System.currentTimeMillis()}"; val saleVal="${System.currentTimeMillis()}|$total|${getProfit()}|$tendered"
            getSharedPreferences("sales_$shopId",0).edit().putString(key,saleVal).apply(); getSharedPreferences("sales_main",0).edit().putString(key,saleVal).apply()
            val cur=getCurrency(); val shopName=getShopNameSet(); val addr=getShopAddrSet(); val phone=getShopPhoneSet(); val header=getReceiptHeaderSet(); val footer=getReceiptFooterSet()
            val sb=StringBuilder(); sb.append("${shopName.uppercase()}\n"); if(addr.isNotEmpty()) sb.append("$addr\n"); if(phone.isNotEmpty()) sb.append("Tel: $phone\n")
            sb.append("--------------------------------\n$header\n--------------------------------\n"); for(c in cart){ sb.append("${c.product.name} x${c.qty} ${cur}${String.format(Locale.getDefault(),"%.2f",c.product.sell*c.qty)}\n") }
            sb.append("--------------------------------\nTOTAL: ${cur}${String.format(Locale.getDefault(),"%.2f",total)}\nTENDERED: ${cur}${String.format(Locale.getDefault(),"%.2f",tendered)}\nCHANGE: ${cur}${String.format(Locale.getDefault(),"%.2f",tendered-total)}\n--------------------------------\n(All Prices Tax Inclusive)\n$footer\n"); lastReceiptText=sb.toString()
            getSharedPreferences("pos",0).edit().putString("last_receipt",lastReceiptText).apply()
        }catch(_:Exception){}
        showReceiptPopup(total,tendered)
    }

    private fun showReceiptPopup(total: Double, tendered: Double){
        val cur=getCurrency(); val view=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(40,30,40,30); setBackgroundColor(Color.WHITE)}
        view.addView(TextView(this).apply{text="✅ Sale Complete! Tax Incl\nTOTAL: $cur${String.format(Locale.getDefault(),"%.2f",total)}\nReceipt Saved"; gravity=Gravity.CENTER; setPadding(0,20,0,20)})
        val btnNew=Button(this).apply{text="➕ NEW SALE"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)}
        view.addView(btnNew); val dlg=AlertDialog.Builder(this).setView(view).create(); btnNew.setOnClickListener{ cart.clear(); tenderedInput.setText(""); refreshCart(); loadRealStock(); dlg.dismiss() }; dlg.show()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        cashierName=getSharedPreferences("pos",0).getString("current_user","Cashier")?:"Cashier"
        try{ val ps=getSharedPreferences("shops_db",0); for((id,raw) in ps.all){ shops.add(Pair(id,raw.toString().split("|")[0])) } }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(8,8,8,8); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        val cur=getCurrency(); root.addView(TextView(this).apply{text="${getShopNameSet()} - POS - $cur Tax Incl"; setTypeface(null,Typeface.BOLD); setPadding(8,8,8,8); setBackgroundColor(Color.parseColor("#BBF7D0"))})
        fun label(t:String)=TextView(this).apply{text=t; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(12,6,12,6)}
        root.addView(label("SELECT SHOP")); spinnerShop=Spinner(this).apply{setBackgroundColor(Color.WHITE)}; spinnerShop.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shops.map{it.second}); root.addView(spinnerShop)
        val searchBtn=Button(this).apply{text="🔍 SEARCH PRODUCTS"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE)}; searchBtn.setOnClickListener{showSearchDialog()}; root.addView(searchBtn)
        root.addView(label("CART - TAX INCLUSIVE")); cartLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)}; val scroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(cartLayout)}; root.addView(scroll)
        val bottomBar=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)}; val receiptRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,10,12,10); setBackgroundColor(Color.parseColor("#E2E0F0"))}
        receiptView=TextView(this).apply{text="RECEIPT: 0 items"; setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f)}; totalView=TextView(this).apply{text="TOTAL: ${cur}0.00"; setTypeface(null,Typeface.BOLD); gravity=Gravity.END; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}; receiptRow.addView(receiptView); receiptRow.addView(totalView); bottomBar.addView(receiptRow)
        val tenderRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(8,6,8,6)}; tenderRow.addView(TextView(this).apply{text="TENDERED ${cur}:"; setTypeface(null,Typeface.BOLD)}); tenderedInput=EditText(this).apply{hint="0.00"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(8,0,0,0)}}; tenderRow.addView(tenderedInput); bottomBar.addView(tenderRow)
        changeView=TextView(this).apply{text="CHANGE: ${cur}0.00"; setBackgroundColor(Color.parseColor("#FEF3C7")); setPadding(12,16,12,16); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER}; bottomBar.addView(changeView)
        bottomBar.addView(Button(this).apply{text="✅ COMPLETE SALE - TAX INCL"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{completeSale()}}); root.addView(bottomBar)
        spinnerShop.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{override fun onItemSelected(a:AdapterView<*>?,v:View?,p:Int,i:Long){loadRealStock()} override fun onNothingSelected(a:AdapterView<*>?){}}
        setContentView(root); loadRealStock(); tenderedInput.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){calcChange()} override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
    }
}