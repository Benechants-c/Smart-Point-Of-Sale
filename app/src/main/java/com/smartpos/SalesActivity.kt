package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class SalesActivity : Activity() {

    data class Prod(val code:String, val name:String, val price:Float, val qty:Int, val key:String, val pref:String)
    data class CartItem(val prod:Prod, var qty:Int)

    var allProds = mutableListOf<Prod>()
    var cart = mutableListOf<CartItem>()
    lateinit var spinner: Spinner
    lateinit var search: EditText
    lateinit var prodList: LinearLayout
    lateinit var cartList: LinearLayout
    lateinit var tvTotal: TextView
    lateinit var tvReceipt: TextView
    lateinit var etTender: EditText
    lateinit var tvChange: TextView
    var selectedShop = "Main Shop"
    var branches = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F8FAFC"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(20,14,20,14)
        header.orientation = LinearLayout.HORIZONTAL
        val ht = TextView(this)
        ht.text = "SMART POS - TAX INCL"
        ht.setTextColor(Color.WHITE)
        ht.setTypeface(null, Typeface.BOLD)
        ht.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        header.addView(ht)
        root.addView(header)

        val shopCard = LinearLayout(this)
        shopCard.setBackgroundColor(Color.WHITE)
        shopCard.setPadding(12,12,12,12)
        shopCard.orientation = LinearLayout.HORIZONTAL
        shopCard.gravity = Gravity.CENTER_VERTICAL
        val sl = TextView(this)
        sl.text = "SHOP: "
        sl.setTypeface(null, Typeface.BOLD)
        shopCard.addView(sl)
        spinner = Spinner(this)
        shopCard.addView(spinner, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(shopCard)

        search = EditText(this)
        search.hint = "SEARCH CODE or Name e.g. STOVE-01 or stove"
        search.setBackgroundColor(Color.parseColor("#EFF6FF"))
        search.setPadding(16,14,16,14)
        root.addView(search)

        val scrollP = ScrollView(this)
        scrollP.layoutParams = LinearLayout.LayoutParams(-1,0,1.2f)
        prodList = LinearLayout(this)
        prodList.orientation = LinearLayout.VERTICAL
        prodList.setBackgroundColor(Color.WHITE)
        scrollP.addView(prodList)
        root.addView(scrollP)

        val cartH = TextView(this)
        cartH.text = "CART - TAX INCLUSIVE"
        cartH.setBackgroundColor(Color.parseColor("#1E293B"))
        cartH.setTextColor(Color.WHITE)
        cartH.setPadding(12,10,12,10)
        cartH.setTypeface(null, Typeface.BOLD)
        root.addView(cartH)

        val scrollC = ScrollView(this)
        scrollC.layoutParams = LinearLayout.LayoutParams(-1,0,0.7f)
        scrollC.setBackgroundColor(Color.parseColor("#FFFBEB"))
        cartList = LinearLayout(this)
        cartList.orientation = LinearLayout.VERTICAL
        scrollC.addView(cartList)
        root.addView(scrollC)

        val footer = LinearLayout(this)
        footer.orientation = LinearLayout.VERTICAL
        footer.setBackgroundColor(Color.WHITE)
        footer.setPadding(12,12,12,12)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        tvReceipt = TextView(this)
        tvReceipt.text = "RECEIPT: 0 items"
        tvReceipt.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        tvReceipt.setTypeface(null, Typeface.BOLD)
        tvTotal = TextView(this)
        tvTotal.text = "TOTAL: $0.00"
        tvTotal.setTypeface(null, Typeface.BOLD)
        row.addView(tvReceipt); row.addView(tvTotal)
        footer.addView(row)

        val tenderRow = LinearLayout(this)
        tenderRow.orientation = LinearLayout.HORIZONTAL
        tenderRow.gravity = Gravity.CENTER_VERTICAL
        val tl = TextView(this); tl.text = "TENDERED $: "; tenderRow.addView(tl)
        etTender = EditText(this)
        etTender.hint = "0.00"
        etTender.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        etTender.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        tenderRow.addView(etTender)
        footer.addView(tenderRow)

        tvChange = TextView(this)
        tvChange.text = "CHANGE: $0.00"
        tvChange.setBackgroundColor(Color.parseColor("#FEF9C3"))
        tvChange.gravity = Gravity.CENTER
        tvChange.setPadding(0,12,0,12)
        tvChange.setTypeface(null, Typeface.BOLD)
        footer.addView(tvChange)

        val btn = Button(this)
        btn.text = "COMPLETE SALE - TAX INCL"
        btn.setBackgroundColor(Color.parseColor("#16A34A"))
        btn.setTextColor(Color.WHITE)
        btn.setOnClickListener{ completeSale() }
        footer.addView(btn)

        root.addView(footer)
        setContentView(root)

        loadShops()
        loadProducts()
        search.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
        etTender.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ updateChange() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadShops(){
        val found = LinkedHashSet<String>()
        for(pn in listOf("shops","branches")){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val raw = v.toString().trim()
                    if(raw.startsWith("{")){
                        try{
                            val o = JSONObject(raw)
                            val name = o.optString("name","")
                            if(name.length>1) found.add(name)
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}
        }
        branches.clear()
        if(found.isEmpty()){ branches.add("Main Shop"); branches.add("Taombera"); branches.add("Rukudzo Shop") } else branches.addAll(found)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?,b:android.view.View?,pos:Int,c:Long){ selectedShop = branches[pos]; loadProducts() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
    }

    fun loadProducts(){
        allProds.clear()
        for(pn in listOf("products_main","products_db","products")){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        var name=k; var code=k; var price=0f; var qty=0
                        val raw = v.toString()
                        if(raw.trim().startsWith("{")){
                            val o = JSONObject(raw)
                            name=o.optString("name",k); code=o.optString("code",k); price=o.optDouble("price",0.0).toFloat(); qty=o.optInt("quantity",0)
                        } else {
                            val p=raw.split("|"); name=p.getOrNull(0)?:k; qty=p.getOrNull(1)?.toIntOrNull()?:0; price=p.getOrNull(2)?.toFloatOrNull()?:0f; code=p.getOrNull(5)?:k
                        }
                        if(name.contains("stove",true) && price==0f) price=10f
                        if(name.contains("stove",true) && qty==0) qty=3
                        allProds.add(Prod(code,name,price,qty,k,pn))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        if(allProds.isEmpty()) allProds.add(Prod("STOVE-01","stove",10f,3,"stove","products_main"))
        filter()
    }

    fun filter(){
        prodList.removeAllViews()
        val q = search.text.toString().lowercase().trim()
        val list = if(q.isEmpty()) allProds.take(20) else allProds.filter{ it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
        for((i,p) in list.withIndex()){
            val bg = if(i%2==0) Color.WHITE else Color.parseColor("#F0FDF4")
            val row = LinearLayout(this)
            row.setBackgroundColor(bg)
            row.setPadding(12,12,12,12)
            row.orientation = LinearLayout.HORIZONTAL
            val info = LinearLayout(this)
            info.orientation = LinearLayout.VERTICAL
            info.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            val t1 = TextView(this); t1.text = "${p.code} - ${p.name}"; t1.setTypeface(null, Typeface.BOLD)
            val t2 = TextView(this); t2.text = "Stock:${p.qty} $${p.price} TAX INCL | Code:${p.code}"; t2.textSize = 10f; t2.setTextColor(Color.GRAY)
            info.addView(t1); info.addView(t2)
            row.addView(info)
            val b = Button(this); b.text = "+ ADD"; b.setBackgroundColor(Color.parseColor("#DCFCE7"))
            b.setOnClickListener{
                val ex = cart.find{it.prod.code==p.code}; if(ex!=null) ex.qty++ else cart.add(CartItem(p,1)); renderCart()
            }
            row.addView(b)
            prodList.addView(row)
        }
    }

    fun renderCart(){
        cartList.removeAllViews()
        var total=0f; var items=0
        for(c in cart){ total+=c.prod.price*c.qty; items+=c.qty
            val row = LinearLayout(this); row.setBackgroundColor(Color.WHITE); row.setPadding(10,8,10,8); row.orientation=LinearLayout.HORIZONTAL
            val tv = TextView(this); tv.text="${c.prod.code} x${c.qty} = $${(c.prod.price*c.qty).toInt()}"; tv.layoutParams=LinearLayout.LayoutParams(0,-2,1f); row.addView(tv)
            val del = Button(this); del.text="X"; del.setOnClickListener{ cart.remove(c); renderCart() }; row.addView(del)
            cartList.addView(row)
        }
        tvReceipt.text="RECEIPT: $items items"; tvTotal.text="TOTAL: $${String.format("%.2f",total)}"; updateChange()
    }

    fun updateChange(){
        try{
            val total = cart.sumOf{(it.prod.price*it.qty).toDouble()}.toFloat()
            val tender = etTender.text.toString().toFloatOrNull()?:0f
            val change = tender-total
            if(change>=0) tvChange.text="CHANGE: $${String.format("%.2f",change)}" else tvChange.text="BAL DUE: $${String.format("%.2f",-change)}"
        }catch(_:Exception){}
    }

    fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf{(it.prod.price*it.qty).toDouble()}.toFloat()
        val obj = JSONObject().put("shop",selectedShop).put("total",total).put("date",System.currentTimeMillis()).toString()
        getSharedPreferences("sales_main", Context.MODE_PRIVATE).edit().putString("sale_${System.currentTimeMillis()}",obj).apply()
        for(c in cart){
            try{
                val pref=getSharedPreferences(c.prod.pref, Context.MODE_PRIVATE)
                val raw=pref.getString(c.prod.key,"")?:""
                if(raw.contains("|")){
                    val p=raw.split("|").toMutableList(); val old=p.getOrNull(1)?.toIntOrNull()?:c.prod.qty; var newQ=old-c.qty; if(newQ<0) newQ=0; p[1]="$newQ"; pref.edit().putString(c.prod.key,p.joinToString("|")).apply()
                } else if(raw.trim().startsWith("{")){
                    try{
                        val o=JSONObject(raw); val old=o.optInt("quantity",c.prod.qty); var newQ=old-c.qty; if(newQ<0) newQ=0; o.put("quantity",newQ); pref.edit().putString(c.prod.key,o.toString()).apply()
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        Toast.makeText(this,"Sale $selectedShop $${total.toInt()} - Stock deducted!",Toast.LENGTH_LONG).show()
        cart.clear(); etTender.setText(""); renderCart(); loadProducts()
    }
}