package com.smartpos

import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

class SystemSettingsActivity : AppCompatActivity() {
    private lateinit var etShopName: EditText
    private lateinit var etShopAddress: EditText
    private lateinit var etShopPhone: EditText
    private lateinit var etCurrency: EditText
    private lateinit var etTax: EditText
    private lateinit var etReceiptFooter: EditText
    private lateinit var etCashier: EditText
    private lateinit var tvStatus: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F5F5")); layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0D47A1")); setPadding(30,25,30,25) }
        val title = TextView(this).apply { text = "SYSTEM SETTINGS"; setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvTime = TextView(this).apply { text = "Real Time: ${sdf.format(Date())} | ${android.os.Build.MODEL}"; setTextColor(Color.parseColor("#BBDEFB")); textSize = 11f }
        header.addView(title); header.addView(tvTime)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        fun createSection(t: String): LinearLayout { val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20); val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,0,20); layoutParams = lp }; val tv = TextView(this).apply { text = t; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK); setPadding(0,0,0,15) }; box.addView(tv); return box }
        fun createEdit(h: String): EditText { return EditText(this).apply { hint = h; setBackgroundResource(android.R.drawable.editbox_background); setPadding(25,25,25,25); val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,8,0,8); layoutParams = lp } }
        val shopBox = createSection("🏪 SHOP INFORMATION"); etShopName = createEdit("Shop Name"); etShopAddress = createEdit("Shop Address"); etShopPhone = createEdit("Shop Phone"); shopBox.addView(etShopName); shopBox.addView(etShopAddress); shopBox.addView(etShopPhone)
        val financeBox = createSection("💰 CURRENCY & TAX"); etCurrency = createEdit("Currency e.g. USD"); etTax = createEdit("Tax % e.g. 15"); financeBox.addView(etCurrency); financeBox.addView(etTax)
        val receiptBox = createSection("🧾 RECEIPT"); etReceiptFooter = createEdit("Receipt Footer"); receiptBox.addView(etReceiptFooter)
        val userBox = createSection("👤 DEFAULT CASHIER"); etCashier = createEdit("Default Cashier Name"); userBox.addView(etCashier)
        tvStatus = TextView(this).apply { textSize = 12f; setTextColor(Color.GRAY); setPadding(0,20,0,0) }
        val btnSave = Button(this).apply { text = "💾 SAVE ALL SETTINGS"; setBackgroundColor(Color.parseColor("#0D47A1")); setTextColor(Color.WHITE) }
        val btnReset = Button(this).apply { text = "RESET"; setBackgroundColor(Color.parseColor("#616161")); setTextColor(Color.WHITE) }
        container.addView(shopBox); container.addView(financeBox); container.addView(receiptBox); container.addView(userBox); container.addView(btnSave); container.addView(btnReset); container.addView(tvStatus)
        scroll.addView(container); root.addView(header); root.addView(scroll); setContentView(root); loadSettings()
        btnSave.setOnClickListener { saveSettings() }; btnReset.setOnClickListener { resetSettings() }
    }
    private fun loadSettings() { val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE); etShopName.setText(prefs.getString("shop_name","")); etShopAddress.setText(prefs.getString("shop_address","")); etShopPhone.setText(prefs.getString("shop_phone","")); etCurrency.setText(prefs.getString("currency","USD")); etTax.setText(prefs.getString("tax_rate","15")); etReceiptFooter.setText(prefs.getString("receipt_footer","Thank you")); etCashier.setText(prefs.getString("cashier_name", prefs.getString("logged_user",""))); tvStatus.text = "Loaded | Real: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}" }
    private fun saveSettings() { val shopName=etShopName.text.toString().trim(); if (shopName.isEmpty()) {etShopName.error="Enter shop name"; return}; val prefs=getSharedPreferences("pos_prefs", MODE_PRIVATE); prefs.edit().putString("shop_name",shopName).putString("shop_address",etShopAddress.text.toString().trim()).putString("shop_phone",etShopPhone.text.toString().trim()).putString("currency",if (etCurrency.text.toString().trim().isEmpty()) "USD" else etCurrency.text.toString().trim()).putString("tax_rate",if (etTax.text.toString().trim().isEmpty()) "15" else etTax.text.toString().trim()).putString("receipt_footer",etReceiptFooter.text.toString().trim()).putString("cashier_name",etCashier.text.toString().trim()).putString("last_settings_update",SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).apply(); Toast.makeText(this, "Settings Saved", Toast.LENGTH_LONG).show() }
    private fun resetSettings() { getSharedPreferences("pos_prefs", MODE_PRIVATE).edit().putString("shop_name","").putString("currency","USD").putString("tax_rate","15").apply(); loadSettings() }
}