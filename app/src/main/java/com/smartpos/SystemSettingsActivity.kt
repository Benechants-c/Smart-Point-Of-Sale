package com.smartpos
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#0D47A1"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "SYSTEM SETTINGS"
        title.setTextColor(Color.WHITE)
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        scroll.layoutParams = lpScroll
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(20,20,20,20)
        scroll.addView(container)

        fun createSection(t: String): LinearLayout{
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setBackgroundColor(Color.WHITE)
            box.setPadding(20,20,20,20)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,0,0,20)
            box.layoutParams = lp
            val tv = TextView(this)
            tv.text = t
            tv.textSize = 14f
            tv.setTypeface(null, Typeface.BOLD)
            box.addView(tv)
            return box
        }

        fun createEdit(h: String): EditText{
            val et = EditText(this)
            et.hint = h
            et.setPadding(25,25,25,25)
            et.setBackgroundColor(Color.parseColor("#FFFFFF"))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,8,0,8)
            et.layoutParams = lp
            return et
        }

        val shopBox = createSection("SHOP INFORMATION")
        etShopName = createEdit("Shop Name")
        etShopAddress = createEdit("Shop Address")
        etShopPhone = createEdit("Shop Phone")
        shopBox.addView(etShopName); shopBox.addView(etShopAddress); shopBox.addView(etShopPhone)

        val finBox = createSection("CURRENCY & TAX")
        etCurrency = createEdit("Currency e.g. USD")
        etTax = createEdit("Tax % e.g. 15")
        finBox.addView(etCurrency); finBox.addView(etTax)

        val recBox = createSection("RECEIPT")
        etReceiptFooter = createEdit("Receipt Footer Message")
        recBox.addView(etReceiptFooter)

        val userBox = createSection("DEFAULT CASHIER")
        etCashier = createEdit("Default Cashier Name")
        userBox.addView(etCashier)

        val btnSave = Button(this)
        btnSave.text = "SAVE ALL SETTINGS"
        btnSave.setBackgroundColor(Color.parseColor("#0D47A1"))
        btnSave.setTextColor(Color.WHITE)

        val btnReset = Button(this)
        btnReset.text = "RESET"
        btnReset.setBackgroundColor(Color.parseColor("#616161"))
        btnReset.setTextColor(Color.WHITE)

        container.addView(shopBox)
        container.addView(finBox)
        container.addView(recBox)
        container.addView(userBox)
        container.addView(btnSave)
        container.addView(btnReset)

        root.addView(header)
        root.addView(scroll)
        setContentView(root)

        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        etShopName.setText(prefs.getString("shop_name",""))
        etShopAddress.setText(prefs.getString("shop_address",""))
        etShopPhone.setText(prefs.getString("shop_phone",""))
        etCurrency.setText(prefs.getString("currency","USD"))
        etTax.setText(prefs.getString("tax_rate","15"))
        etReceiptFooter.setText(prefs.getString("receipt_footer","Thank you!"))
        etCashier.setText(prefs.getString("cashier_name", prefs.getString("logged_user","") ?: ""))

        btnSave.setOnClickListener{
            try{
            val n = etShopName.text.toString().trim()
            if(n.isEmpty()){
                etShopName.error="Enter shop name"
                return@setOnClickListener
            }
            prefs.edit()
                .putString("shop_name", n)
                .putString("shop_address", etShopAddress.text.toString().trim())
                .putString("shop_phone", etShopPhone.text.toString().trim())
                .putString("currency", etCurrency.text.toString().trim())
                .putString("tax_rate", etTax.text.toString().trim())
                .putString("receipt_footer", etReceiptFooter.text.toString().trim())
                .putString("cashier_name", etCashier.text.toString().trim())
                .putString("last_settings_update", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                .apply()
            Toast.makeText(this,"Settings Saved!",Toast.LENGTH_LONG).show()
            }catch(e:Exception){ Toast.makeText(this,"Save error: ${e.message}",Toast.LENGTH_LONG).show() }
        }

        btnReset.setOnClickListener{
            try{
            prefs.edit().putString("shop_name","").putString("shop_address","").putString("shop_phone","").putString("currency","USD").putString("tax_rate","15").apply()
            etShopName.setText(""); etShopAddress.setText(""); etShopPhone.setText(""); etCurrency.setText("USD"); etTax.setText("15")
            Toast.makeText(this,"Reset Done",Toast.LENGTH_SHORT).show()
            }catch(_:Exception){}
        }
        }catch(e:Exception){ Toast.makeText(this,"Init error: ${e.message}",Toast.LENGTH_LONG).show(); finish() }
    }
}