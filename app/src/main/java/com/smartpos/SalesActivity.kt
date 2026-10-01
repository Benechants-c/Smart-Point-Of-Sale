package com.smartpos

import android.app.Activity
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
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()

    private fun getShopId(): String {
        return if (spinnerShop.selectedItemPosition >=0 && spinnerShop.selectedItemPosition < shops.size) shops[spinnerShop.selectedItemPosition].first else "main"
    }
    private fun getShopName(): String {
        return if (spinnerShop.selectedItemPosition >=0 && spinnerShop.selectedItemPosition < shops.size) shops[spinnerShop.selectedItemPosition].second else "Main Shop"
    }
    private fun getTotal(): Double {
        var t = 0.0
        for (c in cart) t += c.product.sell * c.qty
        return t
    }
    private fun calcChange(){
        val total = getTotal()
        val tendered = tenderedInput.text.toString().toDoubleOrNull()?: 0.0
        val change = tendered - total
        if (tendered == 0.0) {
            changeView.text = "CHANGE: $0.00"
            changeView.setTextColor(Color.parseColor("#16A34A"))
        } else if (change < 0) {
            changeView.text = "CHANGE: NEED $${String.format("%.2f", -change)} MORE"
            changeView.setTextColor(Color.RED)
        } else {
            changeView.text = "CHANGE: $${String.format("%.2f", change)}"
            changeView.setTextColor(Color.parseColor("#16A34A"))
        }
    }
    private fun printReceipt(tendered: Double, change: Double){
        if (cart.isEmpty()) return
        var total = 0.0
        var receipt = "============================\n"
        receipt += getShopName().uppercase() + "\nPOS SALES RECEIPT\n"
        receipt += "============================\n"
        for (item in cart){
            val line = item.product.sell * item.qty
            total += line
            receipt += item.product.name + "\n"
            receipt += " " + item.product.code + " x" + item.qty + " @ $" + item.product.sell + " = $" + String.format("%.2f", line) + "\n"
        }
        receipt += "----------------------------\n"
        receipt += "TOTAL: $" + String.format("%.2f", total) + "\n"
        receipt += "TENDERED: $" + String.format("%.2f", tendered) + "\n"
        receipt += "CHANGE: $" + String.format("%.2f", change) + "\n"
        receipt += "============================\nThank you! Come again\n"
        try{
            val i = android.content.Intent(android.content.Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(android.content.Intent.EXTRA_TEXT, receipt)
            startActivity(android.content.Intent.createChooser(i, "PRINT RECEIPT"))
        } catch (e: Exception){
            Toast.makeText(this, receipt, Toast.LENGTH_LONG).show()
        }
    }
    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId = getShopId()
            val stockPref = getSharedPreferences("stock_" + shopId, Context.MODE_PRIVATE)
            for ((code, v) in stockPref.all){
                try{
                    val parts = v.toString().split("|")
                    if (parts.size >= 4){
                        val name = parts[0]
                        val sell = parts[2].toDoubleOrNull()?:
