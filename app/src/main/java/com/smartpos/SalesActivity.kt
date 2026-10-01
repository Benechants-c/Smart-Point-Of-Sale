package com.smartpos

import android.app.Activity
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

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val cost: Double, val sell: Double, var qty: Int, val shopId: String, val dept: String)
    data class CartItem(var product: Product, var qty: Int)

    private val allProducts = mutableListOf<Product>()
    private val filtered = mutableListOf<Product>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var spinnerShop: Spinner
    private lateinit var spinnerDept: Spinner
    private lateinit var cartLayout: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var receiptView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()

    private fun getShopId(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getTotal(): Double { var t=0.0; for(c in cart) t+=c.product.sell*c.qty; return t }
    private fun getProfit(): Double { var p=0.0; for(c in cart) p+=(c.product.sell-c.product.cost)*c.qty; return p }
    private fun calcChange(){
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0; val change=tendered-total
        if(tendered==0.0) changeView.text="CHANGE: $0.00"
        else if(change<0) changeView.text="NEED $${String.format("%.2f",-change)}"
        else changeView.text="CHANGE: $${String.format("%.2f",change)}"
    }

    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val pref=getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            for((code,v) in pref.all){
                try{
                    val p=v.toString().split("|")
                    if(p.size>=4){ val name=p[0]; val cost=p[1].toDoubleOrNull()?:0.0; val sell=p[2].toDoubleOrNull()?:0.0; val qty=p[3].toIntOrNull()?:0; val dept=if(p.size>5)p[5] else "General"; if(qty>=0) allProducts.add(Product(code,name,cost,sell,qty,shopId,dept)) }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        depts.clear(); depts.add("ALL DEPARTMENTS"); for(d in allProducts.map{it.dept.ifEmpty{"General"}}.distinct()) depts.add(d)
        spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); refreshCart()
    }

    private fun showSearchDialog(){
        val dlg=Dialog(this); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE)}
        root.addView(TextView(this).apply{text="Search Product"; textSize=16f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(16,12,16,12)})
        val ed=EditText(this).apply{hint="Enter name or code..."; setPadding(20,14,20,14); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        root.addView(ed); val listLay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val scroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(listLay)}; root.addView(scroll)
        fun refreshList(q:String){
            listLay.removeAllViews(); filtered.clear(); val qq=q.trim().lowercase()
            val selDept=if(spinnerDept.selectedItemPosition in depts.indices) depts[spinnerDept.selectedItemPosition] else "ALL DEPARTMENTS"
            for(p in allProducts){ if((qq.isEmpty()||p.name.lowercase().contains(qq)||p.code.lowercase().contains(qq)) && (selDept=="ALL DEPARTMENTS"||p.dept==selDept)) filtered.add(p) }
            for(p in filtered){
                val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(14,12,14,12); setBackgroundColor(if(p.qty<5) Color.parseColor("#FEF2F2") else Color.WHITE)}
                val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                left.addView(TextView(this).apply{text=p.name + if(p.qty<5)" LOW" else ""; textSize=14f; setTypeface(null,Typeface.BOLD)})
                left.addView(TextView(this).apply{text="${p.code} | Stock:${p.qty}"; textSize=11f; setTextColor(Color.GRAY)})
                val price=TextView(this).apply{text="$${p.sell}"; textSize=14f; setTypeface(null,Typeface.BOLD); gravity=Gravity.END}
                row.addView(left); row.addView(price); row.setOnClickListener{ addToCart(p); dlg.dismiss() }; listLay.addView(row)
                listLay.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))})
            }
        }
        ed.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){refreshList(s.toString())} override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
        refreshList(""); val close=Button(this).apply{text="CLOSE"; setBackgroundColor(Color.parseColor("#E5E7EB"))}; close.setOnClickListener{dlg.dismiss()}; root.addView(close)
        dlg.setContentView(root); dlg.show(); dlg.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(),-2)
    }

    private fun addToCart(p: Product){
        val ex=cart.find{it.product.code==p.code}
        if(ex!=null){ if(ex.qty<p.qty) ex.qty++ else Toast.makeText(this,"Max ${p.qty}",0).show() } else cart.add(CartItem(p,1))
        refreshCart()
    }

    private fun refreshCart(){
        cartLayout.removeAllViews()
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,10,10,10); setBackgroundColor(Color.parseColor("#E2E0F0"))}
        fun h(t:String,w:Float)=TextView(this).apply{text=t; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#475569")); layoutParams=LinearLayout.LayoutParams(0,-2,w)}
        header.addView(TextView(this).apply{text="#"; layoutParams=LinearLayout.LayoutParams(60,-2); setTypeface(null,Typeface.BOLD); textSize=11f})
        header.addView(h("ITEM",2f)); header.addView(h("QTY",1.3f)); header.addView(h("PRICE",1f)); header.addView(h("TOTAL",1f))
        cartLayout.addView(header)
        var total=0.0; val copy = cart.toList()
        for(i in copy.indices){
            val cur = copy[i]; val line=cur.product.sell*cur.qty; total+=line
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,12,10,12); setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL}
            row.addView(TextView(this).apply{text="${i+1}."; layoutParams=LinearLayout.LayoutParams(60,-2); textSize=12f})
            row.addView(TextView(this).apply{text=cur.product.name; layoutParams=LinearLayout.LayoutParams(0,-2,2f); textSize=13f})
            val qtyLay=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(0,-2,1.3f); gravity=Gravity.CENTER}
            val minus=Button(this).apply{text="-"; textSize=20f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(100,100).apply{setMargins(0,0,4,0)}; setOnClickListener{ if(cur.qty>1){cur.qty--} else {cart.remove(cur)}; refreshCart() }}
            val qty=TextView(this).apply{text="${cur.qty}"; textSize=18f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(0,22,0,22); layoutParams=LinearLayout.LayoutParams(70,100).apply{setMargins(0,0,4,0)}}
            val plus=Button(this).apply{text="+"; textSize=20f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(100,100); setOnClickListener{ if(cur.qty < cur.product.qty){cur.qty++; refreshCart()} else Toast.makeText(this@SalesActivity,"Max ${cur.product.qty}",0).show() }}
            qtyLay.addView(minus); qtyLay.addView(qty); qtyLay.addView(plus); row.addView(qtyLay)
            row.addView(TextView(this).apply{text="$${cur.product.sell}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; textSize=12f})
            row.addView(TextView(this).apply{text="$${String.format("%.2f",line)}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.END; setTypeface(null,Typeface.BOLD); textSize=12f})
            cartLayout.addView(row); cartLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))})
        }
        receiptView.text="RECEIPT: ${cart.size} items"; totalView.text="TOTAL: $${String.format("%.2f",total)}"; calcChange()
    }

    private fun completeSale(){
        if(cart.isEmpty()){Toast.makeText(this,"Cart empty",0).show(); return}
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0
        if(tendered<total){Toast.makeText(this,"Tendered less than TOTAL",1).show(); return}
        try{
            val shopId=getShopId()
            for(item in cart){
                val pref=getSharedPreferences("stock_$shopId",Context.MODE_PRIVATE); val old=pref.getString(item.product.code,"")?:""
                if(old.contains("|")){ val parts=old.split("|").toMutableList(); val oq=parts.getOrNull(3)?.toIntOrNull()?:0; parts[3]=(oq-item.qty).toString(); pref.edit().putString(item.product.code,parts.joinToString("|")).apply() }
            }
            val salesPref=getSharedPreferences("sales_$shopId",Context.MODE_PRIVATE)
            val key="SALE_${System.currentTimeMillis()}"; val saleVal="${System.currentTimeMillis()}|$total|${getProfit()}|$tendered"
            salesPref.edit().putString(key,saleVal).apply()
        }catch(_:Exception){}
        Toast.makeText(this,"SALE OK $${String.format("%.2f",total)}",1).show()
        cart.clear(); tenderedInput.setText(""); refreshCart(); loadRealStock()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{ val ps=getSharedPreferences("shops_db",Context.MODE_PRIVATE); for((k,_) in ps.all) shops.add(Pair(k,k)) }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(8,8,8,8); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        // ONLY POS Sales on top - CLEAN
        root.addView(TextView(this).apply{text="POS Sales"; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(8,8,8,8)})
        fun label(t:String)=TextView(this).apply{text=t; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,6,12,6)}
        root.addView(label("SELECT SHOP"))
        spinnerShop=Spinner(this).apply{setBackgroundColor(Color.WHITE)}; spinnerShop.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shops.map{it.second}); root.addView(spinnerShop)
        root.addView(label("DEPARTMENT"))
        spinnerDept=Spinner(this).apply{setBackgroundColor(Color.WHITE)}; depts.add("ALL DEPARTMENTS"); spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); root.addView(spinnerDept)
        // ONLY SEARCH - NO STOCK/ADMIN - CLEAN CASHIER
        val searchBtn=Button(this).apply{text="🔍 SEARCH PRODUCTS"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}; setPadding(0,20,0,20); textSize=16f; setTypeface(null,Typeface.BOLD)}
        searchBtn.setOnClickListener{showSearchDialog()}; root.addView(searchBtn)
        root.addView(label("CART - Use - / +"))
        cartLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)}
        val scroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(cartLayout)}; root.addView(scroll)
        val receiptRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,10,12,10); setBackgroundColor(Color.parseColor("#E2E0F0"))}
        receiptView=TextView(this).apply{text="RECEIPT: 0 items"; setTypeface(null,Typeface.BOLD); textSize=12f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        totalView=TextView(this).apply{text="TOTAL: $0.00"; setTypeface(null,Typeface.BOLD); textSize=14f; gravity=Gravity.END; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        receiptRow.addView(receiptView); receiptRow.addView(totalView); root.addView(receiptRow)
        val tenderRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(8,6,8,6); setBackgroundColor(Color.WHITE)}
        tenderRow.addView(TextView(this).apply{text="TENDERED $:"; setTypeface(null,Typeface.BOLD); textSize=12f})
        tenderedInput=EditText(this).apply{hint="0.00"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; layoutParams=LinearLayout.LayoutParams(-1,-2,1f).apply{setMargins(8,0,0,0)}; setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(12,8,12,8)}; tenderRow.addView(tenderedInput); root.addView(tenderRow)
        changeView=TextView(this).apply{text="CHANGE: $0.00"; setBackgroundColor(Color.parseColor("#FEF3C7")); setPadding(12,16,12,16); setTypeface(null,Typeface.BOLD); textSize=18f; setTextColor(Color.parseColor("#16A34A")); gravity=Gravity.CENTER}
        root.addView(changeView)
        root.addView(Button(this).apply{text="COMPLETE SALE"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setPadding(0,16,0,16); textSize=16f; setTypeface(null,Typeface.BOLD); setOnClickListener{completeSale()}})
        spinnerShop.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{override fun onItemSelected(a:AdapterView<*>?,v:View?,p:Int,i:Long){loadRealStock()} override fun onNothingSelected(a:AdapterView<*>?){}}
        spinnerDept.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{override fun onItemSelected(a:AdapterView<*>?,v:View?,p:Int,i:Long){refreshCart()} override fun onNothingSelected(a:AdapterView<*>?){}}
        setContentView(root); loadRealStock()
        tenderedInput.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){calcChange()} override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
    }
}
