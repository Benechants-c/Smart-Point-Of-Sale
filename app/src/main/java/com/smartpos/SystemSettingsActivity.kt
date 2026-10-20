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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F5F5")); layoutParams = ViewGroup.LayoutParams(-1,-1) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0D47A1")); setPadding(30,25,30,25) }
        val title = TextView(this).apply { text = "SYSTEM SETTINGS"; setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        header.addView(title)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1,0,1f) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        fun sec(t: String): LinearLayout { val b=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,20); layoutParams=lp}; val tv=TextView(this).apply{ text=t; textSize=14f; typeface=Typeface.DEFAULT_BOLD}; b.addView(tv); return b }
        fun edit(h: String): EditText { return EditText(this).apply{ hint=h; setBackgroundResource(android.R.drawable.editbox_background); setPadding(25,25,25,25); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); layoutParams=lp } }
        val shopBox=sec("🏪 SHOP"); etShopName=edit("Shop Name"); etShopAddress=edit("Address"); etShopPhone=edit("Phone"); shopBox.addView(etShopName); shopBox.addView(etShopAddress); shopBox.addView(etShopPhone)
        val finBox=sec("💰 CURRENCY & TAX"); etCurrency=edit("Currency USD"); etTax=edit("Tax %"); finBox.addView(etCurrency); finBox.addView(etTax)
        val recBox=sec("🧾 RECEIPT"); etReceiptFooter=edit("Footer"); recBox.addView(etReceiptFooter)
        val userBox=sec("👤 CASHIER"); etCashier=edit("Cashier"); userBox.addView(etCashier)
        val btnSave=Button(this).apply{ text="💾 SAVE SETTINGS"; setBackgroundColor(Color.parseColor("#0D47A1")); setTextColor(Color.WHITE)}
        container.addView(shopBox); container.addView(finBox); container.addView(recBox); container.addView(userBox); container.addView(btnSave)
        scroll.addView(container); root.addView(header); root.addView(scroll); setContentView(root)
        val prefs=getSharedPreferences("pos_prefs",MODE_PRIVATE); etShopName.setText(prefs.getString("shop_name","")); etShopAddress.setText(prefs.getString("shop_address","")); etShopPhone.setText(prefs.getString("shop_phone","")); etCurrency.setText(prefs.getString("currency","USD")); etTax.setText(prefs.getString("tax_rate","15")); etReceiptFooter.setText(prefs.getString("receipt_footer","Thank you")); etCashier.setText(prefs.getString("cashier_name",prefs.getString("logged_user","")))
        btnSave.setOnClickListener{ val n=etShopName.text.toString().trim(); if(n.isEmpty()){ etShopName.error="Enter name"; return@setOnClickListener}; prefs.edit().putString("shop_name",n).putString("shop_address",etShopAddress.text.toString()).putString("shop_phone",etShopPhone.text.toString()).putString("currency",etCurrency.text.toString()).putString("tax_rate",etTax.text.toString()).putString("receipt_footer",etReceiptFooter.text.toString()).putString("cashier_name",etCashier.text.toString()).apply(); Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show()}
    }
}