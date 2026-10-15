package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class SalesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = TextView(this)
        header.text = "SMART POS - TAX INCL - PASTEL"
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setTextColor(Color.WHITE)
        header.setPadding(20,14,20,14)
        header.setTypeface(null, Typeface.BOLD)
        root.addView(header)

        val shopRow = LinearLayout(this)
        shopRow.setBackgroundColor(Color.WHITE)
        shopRow.setPadding(12,12,12,12)
        shopRow.orientation = LinearLayout.HORIZONTAL
        val shopLabel = TextView(this)
        shopLabel.text = "SELECT SHOP: "
        shopLabel.setTypeface(null, Typeface.BOLD)
        shopRow.addView(shopLabel)
        val spinner = Spinner(this)
        val shops = arrayOf("Main Shop","Taombera","Rukudzo Shop")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        shopRow.addView(spinner)
        root.addView(shopRow)

        val search = EditText(this)
        search.hint = "SEARCH by CODE SKU or Name e.g. STOVE-01"
        search.setBackgroundColor(Color.parseColor("#EFF6FF"))
        search.setPadding(16,14,16,14)
        root.addView(search)

        val prodCard = TextView(this)
        prodCard.text = "📦 STOVE-01 - stove\nStock: 3 | $10 TAX INCL | Code: STOVE-01\n[+ ADD] button here"
        prodCard.setBackgroundColor(Color.WHITE)
        prodCard.setPadding(16,16,16,16)
        prodCard.setTypeface(null, Typeface.BOLD)
        root.addView(prodCard)

        val cartHeader = TextView(this)
        cartHeader.text = "CART - TAX INCLUSIVE - PASTEL YELLOW"
        cartHeader.setBackgroundColor(Color.parseColor("#FEF9C3"))
        cartHeader.setPadding(12,10,12,10)
        cartHeader.setTypeface(null, Typeface.BOLD)
        root.addView(cartHeader)

        val cart = TextView(this)
        cart.text = "RECEIPT: 1 items TOTAL: $10.00\nTENDERED $: 20\nCHANGE: $10.00\n✅ COMPLETE SALE"
        cart.setBackgroundColor(Color.parseColor("#FFFBEB"))
        cart.setPadding(16,20,16,20)
        cart.gravity = Gravity.CENTER
        root.addView(cart)

        val btn = Button(this)
        btn.text = "BACK"
        btn.setOnClickListener{ finish() }
        root.addView(btn)

        setContentView(root)
    }
}