package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val sell: Double, var qty: Int, val shopId: String, val dept: String)
    data class CartItem(var product: Product, var qty: Int)

    private val allProducts = mutableListOf<Product>()
    private val filtered = mutableListOf<Product>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var spinnerShop: Spinner
    private lateinit var spinnerDept: Spinner
    private lateinit var searchInput: EditText
    private lateinit var productsLayout: LinearLayout
    private lateinit var cartLayout: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var receiptView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()

    private fun getShopId(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getShopName(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].second else "Main Shop"
    private fun getTotal(): Double { var t=0.0; for(c in cart) t+=c.product.sell*c.qty; return t }
    private fun calcChange(){
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0; val change=tendered-total
        if(tendered==0.0){ changeView.text="CHANGE: $0.00"; changeView.setTextColor(Color.parseColor("#16A34A")) }
        else if(change<0){ changeView.text="NEED $${String.format("%.2f",-change)} MORE"; changeView.setTextColor(Color.RED) }
        else { changeView.text="CHANGE: $${String.format("%.2f",change)}"; changeView.setTextColor(Color.parseColor("#16A34A")) }
    }

    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val stockPref=getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            for((code,v) in stockPref.all){
                try{
                    val parts=v.toString().split("|")
                    if(parts.size>=4){ val name=parts[0]; val sell=parts[2].toDoubleOrNull()?:0.0; val qty=parts[3].toIntOrNull()?:0; val dept=if(parts.size>5) parts[5] else "General"; if(qty>0) allProducts.add(Product(code,name,sell,qty,shopId,dept)) }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        depts.clear(); depts.add("ALL DEPARTMENTS"); for(d in allProducts.map{it.dept.ifEmpty{"General"}}.distinct()) depts.add(d)
        spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); applyFilter()
    }
    private fun applyFilter(){
        filtered.clear(); val q=searchInput.text.toString().trim().lowercase(); val deptSel=if(spinnerDept.selectedItemPosition in depts.indices) depts[spinnerDept.selectedItemPosition] else "ALL DEPARTMENTS"
        for(p in allProducts){ val ms=q.isEmpty()||p.name.lowercase().contains(q)||p.code.lowercase().contains(q); val md=deptSel=="ALL DEPARTMENTS"||p.dept==deptSel; if(ms&&md) filtered.add(p) }
        refreshProducts()
    }
    private fun refreshProducts(){
        productsLayout.removeAllViews()
        if(filtered.isEmpty()){ productsLayout.addView(TextView(this).apply{text="No stock in ${getShopName()}"; setPadding(20,40,20,40); setTextColor(Color.GRAY); textSize=14f}); return }
        for(p in filtered){
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(2,2,2,2); setBackgroundColor(Color.parseColor("#E5E7EB"))}
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(14,14,14,14); setBackgroundColor(Color.WHITE)}
            val info=TextView(this).apply{
                text="${p.name}\n${p.code} | Stock:${p.qty} | $${p.sell}"; textSize=13f; setTextColor(Color.parseColor("#111827"))
                layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,12,0)}
            }
            val btn=Button(this).apply{
                text="ADD"; textSize=12f; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
                layoutParams=LinearLayout.LayoutParams(180,-2)
            }
            btn.setOnClickListener{addToCart(p)}
            row.addView(info); row.addView(btn); card.addView(row)
            productsLayout.addView(card)
            productsLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,8)})
        }
    }
    private fun addToCart(p: Product){
        val exist=cart.find{it.product.code==p.code}
        if(exist!=null){ if(exist.qty<p.qty) exist.qty++ else Toast.makeText(this,"No more stock",Toast.LENGTH_SHORT).show() } else cart.add(CartItem(p,1))
        refreshCart()
    }

    private fun refreshCart(){
        cartLayout.removeAllViews(); var total=0.0; var tq=0; var receiptTxt=""
        if(cart.isEmpty()){
            cartLayout.addView(TextView(this).apply{text="Cart empty"; setPadding(20,20,20,20); setTextColor(Color.GRAY)})
        }
        for(item in cart){
            val line=item.product.sell*item.qty; total+=line; tq+=item.qty
            receiptTxt+="${item.product.name} x${item.qty} = $${String.format("%.2f",line)}\n"
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(1,1,1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))}
            val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,10,10,10); setBackgroundColor(Color.WHITE); gravity=android.view.Gravity.CENTER_VERTICAL}
            val nameTv=TextView(this).apply{
                text="${item.product.name}\n$${item.product.sell} x${item.qty} = $${String.format("%.2f",line)}"
                textSize=12f; setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            }
            val minus=Button(this).apply{text="-"; setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(120,-2)}
            val qtyTv=TextView(this).apply{text="${item.qty}"; setPadding(16,8,16,8); setTypeface(null,Typeface.BOLD); textSize=14f; gravity=android.view.Gravity.CENTER}
            val plus=Button(this).apply{text="+"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(120,-2)}
            minus.setOnClickListener{ if(item.qty>1){item.qty--; refreshCart()} else {cart.remove(item); refreshCart()} }
            plus.setOnClickListener{ if(item.qty<item.product.qty){item.qty++; refreshCart()} else Toast.makeText(this,"Max ${item.product.qty}",Toast.LENGTH_SHORT).show() }
            r.addView(nameTv); r.addView(minus); r.addView(qtyTv); r.addView(plus); card.addView(r)
            cartLayout.addView(card)
            cartLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,6)})
        }
        if(cart.isEmpty()) receiptView.text="RECEIPT: No items"
        else receiptView.text=receiptTxt+"----\nTOTAL: $${String.format("%.2f",total)} | ITEMS: $tq"
        totalView.text="TOTAL: $${String.format("%.2f",total)} | $tq items | ${getShopName()}"; calcChange()
    }

    private fun completeSale(){
        if(cart.isEmpty()){Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return}
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0
        if(tendered<total){Toast.makeText(this,"Tendered less than TOTAL",Toast.LENGTH_LONG).show(); return}
        val change=tendered-total
        try{
            val shopId=getShopId()
            for(item in cart){
                val stockPref=getSharedPreferences("stock_$shopId",Context.MODE_PRIVATE)
                val old=stockPref.getString(item.product.code,"")?:""
                if(old.contains("|")){
                    val parts=old.split("|").toMutableList(); val oldQty=parts.getOrNull(3)?.toIntOrNull()?:0; parts[3]=(oldQty-item.qty).toString()
                    stockPref.edit().putString(item.product.code,parts.joinToString("|")).apply()
                }
            }
        }catch(_:Exception){}
        Toast.makeText(this,"SALE COMPLETE Change $${String.format("%.2f",change)}",Toast.LENGTH_LONG).show()
        cart.clear(); tenderedInput.setText(""); refreshCart(); loadRealStock()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{ val ps=getSharedPreferences("shops_db",Context.MODE_PRIVATE); for((k,v) in ps.all){ shops.add(Pair(k,k)) } }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(12,12,12,12); setBackgroundColor(Color.parseColor("#F8FAFC"))}
        root.addView(TextView(this).apply{text="POS SALES - BUILD 159 PRETTY"; textSize=16f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,8)})

        fun label(t:String)=TextView(this).apply{text=t; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,8,12,8)}

        root.addView(label("SELECT SHOP"))
        spinnerShop=Spinner(this).apply{setBackgroundColor(Color.WHITE)}; spinnerShop.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shops.map{it.second}); root.addView(spinnerShop)
        root.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,8)})

        root.addView(label("DEPARTMENT"))
        spinnerDept=Spinner(this).apply{setBackgroundColor(Color.WHITE)}; depts.add("ALL DEPARTMENTS"); spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); root.addView(spinnerDept)
        root.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,8)})

        searchInput=EditText(this).apply{hint="Search product"; setPadding(18,14,18,14); setBackgroundColor(Color.WHITE); textSize=14f}; root.addView(searchInput)
        root.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,8)})

        root.addView(label("PRODUCTS"))
        productsLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};
        val prodScroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(productsLayout); setBackgroundColor(Color.WHITE)}; root.addView(prodScroll)

        root.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,12)})
        root.addView(label("CART - Use - / + to change qty"))
        cartLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9"))};
        val cartScroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,0.7f); addView(cartLayout); setBackgroundColor(Color.parseColor("#F1F5F9"))}; root.addView(cartScroll)

        receiptView=TextView(this).apply{text="RECEIPT: No items"; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(14,12,14,12); setTypeface(null,Typeface.BOLD); textSize=12f; setTextColor(Color.parseColor("#0F172A"))}; root.addView(receiptView)
        totalView=TextView(this).apply{text="TOTAL: $0.00"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(14,12,14,12); setTypeface(null,Typeface.BOLD); textSize=14f}; root.addView(totalView)

        val tenderRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(0,8,0,0); gravity=android.view.Gravity.CENTER_VERTICAL}
        tenderRow.addView(TextView(this).apply{text="TENDERED $: "; setTypeface(null,Typeface.BOLD); textSize=13f})
        tenderedInput=EditText(this).apply{hint="0.00"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setBackgroundColor(Color.WHITE); setPadding(14,10,14,10)}; tenderRow.addView(tenderedInput); root.addView(tenderRow)

        changeView=TextView(this).apply{text="CHANGE: $0.00"; setBackgroundColor(Color.parseColor("#FEF3C7")); setPadding(14,12,14,12); setTypeface(null,Typeface.BOLD); textSize=15f; setTextColor(Color.parseColor("#16A34A"))}; root.addView(changeView)
        root.addView(Button(this).apply{text="COMPLETE SALE + PRINT"; textSize=14f; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setPadding(0,16,0,16); setOnClickListener{completeSale()}})

        spinnerShop.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{override fun onItemSelected(a: AdapterView<*>?,v: View?,p:Int,id:Long){loadRealStock()} override fun onNothingSelected(a: AdapterView<*>?){}}
        spinnerDept.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{override fun onItemSelected(a: AdapterView<*>?,v: View?,p:Int,id:Long){applyFilter()} override fun onNothingSelected(a: AdapterView<*>?){}}
        searchInput.addTextChangedListener(object: TextWatcher{override fun afterTextChanged(s: Editable?){applyFilter()} override fun beforeTextChanged(s: CharSequence?,st:Int,c:Int,a:Int){} override fun onTextChanged(s: CharSequence?,st:Int,b:Int,c:Int){}})
        tenderedInput.addTextChangedListener(object: TextWatcher{override fun afterTextChanged(s: Editable?){calcChange()} override fun beforeTextChanged(s: CharSequence?,st:Int,c:Int,a:Int){} override fun onTextChanged(s: CharSequence?,st:Int,b:Int,c:Int){}})
        setContentView(root); loadRealStock()
    }
}
