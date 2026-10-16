package com.smartpos
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.ByteArrayOutputStream

class SystemSettingsActivity : Activity() {

    private lateinit var menuDropdown: LinearLayout
    private lateinit var menuButton: Button
    private lateinit var receiptSection: LinearLayout
    private lateinit var printerSection: LinearLayout
    private lateinit var otherSection: LinearLayout
    private lateinit var logoImage: ImageView
    private lateinit var receiptPreview: LinearLayout
    private var menuVisible = false
    private var logoBase64: String = ""
    private val PICK_LOGO = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(20,16,20,16)
        header.addView(TextView(this).apply {
            text = "System Settings - TAX INCLUSIVE"
            textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        })
        root.addView(header)

        // BACK - ARRANGED
        root.addView(Button(this).apply {
            text = "BACK"
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setPadding(0,18,0,18)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
            setOnClickListener{ finish() }
        })

        // MENU BUTTON - ARRANGED
        val topBox = LinearLayout(this)
        topBox.orientation = LinearLayout.VERTICAL
        topBox.setBackgroundColor(Color.WHITE)
        topBox.setPadding(12,12,12,12)
        topBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)}

        menuButton = Button(this)
        menuButton.text = "SETTINGS MENU ▼"
        menuButton.setBackgroundColor(Color.parseColor("#0F172A"))
        menuButton.setTextColor(Color.WHITE)
        menuButton.setPadding(0,20,0,20)
        topBox.addView(menuButton)

        menuDropdown = LinearLayout(this)
        menuDropdown.orientation = LinearLayout.VERTICAL
        menuDropdown.visibility = View.GONE
        menuDropdown.setPadding(0,12,0,0)

        fun makeBtn(title:String, color:String, act:()->Unit): Button {
            val b = Button(this)
            b.text = title
            b.setBackgroundColor(Color.parseColor(color))
            b.setTextColor(Color.WHITE)
            b.setPadding(0,20,0,20)
            b.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,6,0,6)}
            b.setOnClickListener{
                menuDropdown.visibility = View.GONE
                menuVisible = false
                menuButton.text = "SETTINGS MENU ▼"
                act()
            }
            return b
        }

        menuDropdown.addView(makeBtn("RECEIPT SETTINGS - Logo & Shop Name","#2563EB"){ showTab("RECEIPT") })
        menuDropdown.addView(makeBtn("PRINTERS - Tax & VAT & Test Print","#16A34A"){ showTab("PRINTERS") })
        menuDropdown.addView(makeBtn("OTHER SETTINGS","#334155"){ showTab("OTHER") })

        menuButton.setOnClickListener{
            menuVisible = !menuVisible
            menuDropdown.visibility = if(menuVisible) View.VISIBLE else View.GONE
            menuButton.text = if(menuVisible) "SETTINGS MENU ▲" else "SETTINGS MENU ▼"
        }

        topBox.addView(menuDropdown)
        root.addView(topBox)

        val scroll = ScrollView(this)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL

        val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        logoBase64 = pref.getString("shop_logo","") ?: ""

        // RECEIPT
        receiptSection = LinearLayout(this)
        receiptSection.orientation = LinearLayout.VERTICAL
        receiptSection.setBackgroundColor(Color.WHITE)
        receiptSection.setPadding(16,16,16,16)
        receiptSection.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}

        receiptSection.addView(TextView(this).apply { text="Shop Information"; setTypeface(null, Typeface.BOLD); textSize=14f; setPadding(0,0,0,12) })

        logoImage = ImageView(this)
        logoImage.layoutParams = LinearLayout.LayoutParams(120,120).apply{gravity=Gravity.CENTER}
        logoImage.setBackgroundColor(Color.parseColor("#F1F5F9"))
        try{
            if(logoBase64.isNotEmpty()){
                val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                logoImage.setImageBitmap(bmp)
            }
        }catch(_:Exception){}
        receiptSection.addView(logoImage)

        receiptSection.addView(Button(this).apply {
            text="PICK LOGO FROM GALLERY"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            setPadding(0,16,0,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}
            setOnClickListener{
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_LOGO)
            }
        })

        receiptSection.addView(Button(this).apply {
            text="REMOVE LOGO"; setBackgroundColor(Color.parseColor("#E2E8F0"))
            setPadding(0,12,0,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,16)}
            setOnClickListener{
                logoBase64=""; pref.edit().putString("shop_logo","").apply()
                logoImage.setImageResource(android.R.drawable.ic_menu_gallery)
                updatePreview()
            }
        })

        fun addField(label:String, key:String, def:String): EditText {
            receiptSection.addView(TextView(this).apply { text=label; textSize=12f; setTextColor(Color.GRAY); setPadding(0,12,0,4) })
            val et = EditText(this)
            et.setText(pref.getString(key,def))
            et.setBackgroundColor(Color.parseColor("#F8FAFC"))
            et.setPadding(16,14,16,14)
            et.setOnFocusChangeListener{_, hasFocus ->
                if(!hasFocus){ pref.edit().putString(key, et.text.toString()).apply(); updatePreview() }
            }
            receiptSection.addView(et)
            return et
        }

        val nameEt = addField("Shop Name","shop_name","ruvheneko")
        val addrEt = addField("Address","shop_address","Karoi")
        val phoneEt = addField("Phone","shop_phone","+263773996805")
        addField("Currency","currency","$")
        addField("Footer","receipt_footer","Thank you!")

        receiptSection.addView(TextView(this).apply { text="RECEIPT PREVIEW"; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#16A34A")); setPadding(0,20,0,8) })
        receiptPreview = LinearLayout(this)
        receiptPreview.orientation = LinearLayout.VERTICAL
        receiptPreview.setBackgroundColor(Color.WHITE)
        receiptPreview.setPadding(16,16,16,16)
        receiptSection.addView(receiptPreview)

        val watcher = object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?){ updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, st:Int, c:Int, a:Int){}
            override fun onTextChanged(s: CharSequence?, st:Int, b:Int, c:Int){}
        }
        nameEt.addTextChangedListener(watcher); addrEt.addTextChangedListener(watcher); phoneEt.addTextChangedListener(watcher)

        content.addView(receiptSection)

        // PRINTERS - FROM MY CODE - ADDED BACK
        printerSection = LinearLayout(this)
        printerSection.orientation = LinearLayout.VERTICAL
        printerSection.setBackgroundColor(Color.WHITE)
        printerSection.setPadding(16,16,16,16)
        printerSection.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
        printerSection.visibility = View.GONE

        printerSection.addView(TextView(this).apply { text="Printer & Tax Settings"; setTypeface(null, Typeface.BOLD); textSize=14f; setPadding(0,0,0,12) })

        val taxRow = LinearLayout(this)
        taxRow.orientation = LinearLayout.HORIZONTAL
        taxRow.gravity = Gravity.CENTER_VERTICAL
        taxRow.addView(TextView(this).apply { text="Tax Inclusive"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val switchTax = Switch(this)
        switchTax.isChecked = pref.getBoolean("tax_inclusive", true)
        switchTax.setOnCheckedChangeListener{_, c-> pref.edit().putBoolean("tax_inclusive",c).apply(); updatePreview() }
        taxRow.addView(switchTax)
        printerSection.addView(taxRow)

        val vatRow = LinearLayout(this)
        vatRow.orientation = LinearLayout.HORIZONTAL
        vatRow.setPadding(0,12,0,0)
        vatRow.addView(TextView(this).apply { text="VAT %"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val etVat = EditText(this)
        etVat.setText(pref.getString("vat_percent","15"))
        etVat.layoutParams = LinearLayout.LayoutParams(120,-2)
        etVat.setBackgroundColor(Color.parseColor("#F1F5F9"))
        vatRow.addView(etVat)
        printerSection.addView(vatRow)

        printerSection.addView(TextView(this).apply { text="Printer Type"; setTextColor(Color.GRAY); setPadding(0,16,0,4) })
        val spinPrint = Spinner(this)
        spinPrint.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("No Printer","Bluetooth","USB","Network"))
        printerSection.addView(spinPrint)

        printerSection.addView(Button(this).apply {
            text="TEST PRINT"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            setPadding(0,20,0,20); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,16,0,8)}
            setOnClickListener{ Toast.makeText(this@SystemSettingsActivity,"Test Print: ${spinPrint.selectedItem}", Toast.LENGTH_SHORT).show() }
        })

        printerSection.addView(Button(this).apply {
            text="SAVE SETTINGS"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            setPadding(0,20,0,20)
            setOnClickListener{ pref.edit().putString("vat_percent", etVat.text.toString()).apply(); Toast.makeText(this@SystemSettingsActivity,"Saved", Toast.LENGTH_SHORT).show() }
        })

        content.addView(printerSection)

        // OTHER
        otherSection = LinearLayout(this)
        otherSection.orientation = LinearLayout.VERTICAL
        otherSection.setBackgroundColor(Color.WHITE)
        otherSection.setPadding(16,16,16,16)
        otherSection.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
        otherSection.visibility = View.GONE
        otherSection.addView(TextView(this).apply { text="Other Settings"; setTypeface(null, Typeface.BOLD) })
        content.addView(otherSection)

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        showTab("RECEIPT")
        updatePreview()
    }

    private fun updatePreview(){
        try{
            val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val name = pref.getString("shop_name","ruvheneko") ?: "ruvheneko"
            receiptPreview.removeAllViews()
            receiptPreview.addView(TextView(this).apply { text=name; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER; textSize=16f })
            receiptPreview.addView(TextView(this).apply { text=pref.getString("shop_address","Karoi"); gravity=Gravity.CENTER; textSize=10f })
            receiptPreview.addView(TextView(this).apply { text="VAT ${pref.getString("vat_percent","15")}% ${if(pref.getBoolean("tax_inclusive",true)) "Inclusive" else "Exclusive"}"; gravity=Gravity.CENTER; textSize=9f; setTextColor(Color.RED) })
            receiptPreview.addView(TextView(this).apply { text="TOTAL $10.00"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD) })
        }catch(_:Exception){}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_LOGO && resultCode==RESULT_OK && data!=null){
            try{
                val uri: Uri? = data.data
                val input = contentResolver.openInputStream(uri!!)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                val baos = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
                logoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("shop_logo", logoBase64).apply()
                logoImage.setImageBitmap(bitmap)
                updatePreview()
            }catch(_:Exception){}
        }
    }

    private fun showTab(tab:String){
        receiptSection.visibility = if(tab=="RECEIPT") View.VISIBLE else View.GONE
        printerSection.visibility = if(tab=="PRINTERS") View.VISIBLE else View.GONE
        otherSection.visibility = if(tab=="OTHER") View.VISIBLE else View.GONE
    }
}