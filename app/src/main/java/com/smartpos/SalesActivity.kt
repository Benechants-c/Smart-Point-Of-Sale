package com.smartpos
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : AppCompatActivity() {

    private lateinit var tvCashier: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvTotal: TextView
    private lateinit var productContainer: LinearLayout
    private lateinit var cartContainer: LinearLayout
    private var cartTotal = 0.0
    private val cartItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#1976D2"))
        header.setPadding(30,30,30,30)

        val title = TextView(this)
        title.text = "SMART POS - SALES"
        title.setTextColor(Color.WHITE)
        title.textSize = 20f
        title.setTypeface(null, Typeface.BOLD)

        tvCashier = TextView(this)
        tvCashier.setTextColor(Color.WHITE)
        tvCashier.textSize = 14f
        try{
            val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
            val cashierName = prefs.getString("cashier_name", null) ?: prefs.getString("logged_user", "Cashier") ?: "Cashier"
            tvCashier.text = "Cashier: $cashierName"
        }catch(_:Exception){ tvCashier.text = "Cashier: Cashier" }

        tvDateTime = TextView(this)
        tvDateTime.setTextColor(Color.parseColor("#E3F2FD"))
        tvDateTime.textSize = 13f
        tvDateTime.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        header.addView(title)
        header.addView(tvCashier)
        header.addView(tvDateTime)

        val productLabel = TextView(this)
        productLabel.text = "Products (From Goods Received)"
        productLabel.setPadding(20,20,20,10)
        productLabel.textSize = 16f
        productLabel.setTypeface(null, Typeface.BOLD)

        val scrollProducts = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lpScroll.weight = 1f
        scrollProducts.layoutParams = lpScroll

        productContainer = LinearLayout(this)
        productContainer.orientation = LinearLayout.VERTICAL
        productContainer.setPadding(15,15,15,15)
        scrollProducts.addView(productContainer)

        val cartLabel = TextView(this)
        cartLabel.text = "Cart"
        cartLabel.setPadding(20,15,20,10)
        cartLabel.textSize = 16f
        cartLabel.setTypeface(null, Typeface.BOLD)
        cartLabel.setBackgroundColor(Color.WHITE)

        cartContainer = LinearLayout(this)
        cartContainer.orientation = LinearLayout.VERTICAL
        cartContainer.setBackgroundColor(Color.WHITE)
        cartContainer.setPadding(20,20,20,20)

        tvTotal = TextView(this)
        tvTotal.text = "Total: $0.00"
        tvTotal.textSize = 18f
        tvTotal.setTypeface(null, Typeface.BOLD)
        tvTotal.setTextColor(Color.parseColor("#1976D2"))
        tvTotal.setPadding(20,10,20,20)
        tvTotal.setBackgroundColor(Color.WHITE)

        val btnCheckout = Button(this)
        btnCheckout.text = "CHECKOUT"
        btnCheckout.setBackgroundColor(Color.parseColor("#4CAF50"))
        btnCheckout.setTextColor(Color.WHITE)

        btnCheckout.setOnClickListener {
            try{
            if(cartItems.isEmpty()){
                Toast.makeText(this, "Cart empty", Toast.LENGTH_SHORT).show()
            }else{
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                Toast.makeText(this, "Sale: $cartTotal - $time", Toast.LENGTH_LONG).show()
                cartItems.clear()
                cartTotal = 0.0
                refreshCart()
            }
            }catch(_:Exception){}
        }

        root.addView(header)
        root.addView(productLabel)
        root.addView(scrollProducts)
        root.addView(cartLabel)
        root.addView(cartContainer)
        root.addView(tvTotal)
        root.addView(btnCheckout)

        setContentView(root)
        loadGoodsReceivedProducts()

        }catch(e:Exception){
            Toast.makeText(this,"Sales init error: ${e.message}",Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadGoodsReceivedProducts(){
        try{
        productContainer.removeAllViews()
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val productList = mutableListOf<Pair<String, Double>>()

        for((_, value) in goodsPrefs.all){
            try{
                val s = value.toString()
                if(s.contains("|")){
                    val parts = s.split("|")
                    val name = parts.getOrNull(1) ?: s
                    val price = parts.getOrNull(4)?.toDoubleOrNull() ?: parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                    productList.add(Pair(name, price))
                }else{
                    val price = s.toDoubleOrNull() ?: 1.0
                    productList.add(Pair(value.toString(), price))
                }
            }catch(_:Exception){}
        }

        if(productList.isEmpty()){
            productList.add(Pair("Coca Cola 500ml", 1.50))
            productList.add(Pair("Bread Loaf", 1.20))
            productList.add(Pair("Milk 1L", 1.00))
        }

        for((name, price) in productList){
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(15,15,15,15)
            row.setBackgroundColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0,0,0,15)
            row.layoutParams = params

            val tvName = TextView(this)
            tvName.text = "$name - $${String.format("%.2f", price)}"
            tvName.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            tvName.setTextColor(Color.BLACK)

            val btnAdd = Button(this)
            btnAdd.text = "ADD"
            btnAdd.setBackgroundColor(Color.parseColor("#1976D2"))
            btnAdd.setTextColor(Color.WHITE)

            btnAdd.setOnClickListener {
                try{
                cartItems.add(name)
                cartTotal += price
                refreshCart()
                Toast.makeText(this, "$name added", Toast.LENGTH_SHORT).show()
                }catch(_:Exception){}
            }

            row.addView(tvName)
            row.addView(btnAdd)
            productContainer.addView(row)
        }
        }catch(_:Exception){ Toast.makeText(this,"Load error",Toast.LENGTH_SHORT).show() }
    }

    private fun refreshCart(){
        try{
        cartContainer.removeAllViews()
        for(item in cartItems){
            val tv = TextView(this)
            tv.text = "• $item"
            tv.setPadding(5,5,5,5)
            cartContainer.addView(tv)
        }
        tvTotal.text = "Total: $${String.format("%.2f", cartTotal)}"
        }catch(_:Exception){}
    }
}