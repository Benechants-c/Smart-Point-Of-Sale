package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {

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
            val cashierName = prefs.getString("cashier_name", null) ?: prefs