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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // HEADER
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16)
        }
        header.addView(TextView(this).apply {
            text = "⚙️ System Settings - TAX INCLUSIVE"
            textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        })
        root.addView(header)

        // BACK BUTTON - ARRANGED
        root.addView(Button(this).apply {
            text = "BACK"; textSize = 12f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#E2E8F0")); setTextColor(Color.parseColor("#334155"))
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
            setPadding(0,18,0,18)
            setOnClickListener{ finish() }
        })

        // SETTINGS MENU BUTTON - ARRANGED CENTER
        val topMenuBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(12,12,12,12)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)}
            elevation = 4f
        }

        menuButton = Button(this).apply {
            text = "⚙️ SETTINGS MENU ▼"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#0F172A")); setTextColor(Color.WHITE)
            setPadding(0,20,0,20)
            layoutParams = LinearLayout.LayoutParams(-1,-2)
            setOnClickListener{
                menuVisible = !menuVisible
                menuDropdown.visibility = if(menuVisible) View.VISIBLE else View.GONE
                text = if(menuVisible) "⚙️ SETTINGS MENU ▲" else "⚙️ SETTINGS MENU ▼"
            }
        }
        topMenuBox.addView(menuButton)

        // DROPDOWN - 3 BUTTONS ARRANGED PROPERLY
        menuDropdown = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE
            setPadding(0,12,0,0)
        }

        // ARRANGED BUTTON ROW - GRID STYLE
        fun dropBtn(title:String, color:String, action:()->Unit): Button {
            return Button(this).apply {
                text = title; textSize = 12f; setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE)
                setPadding(0,22,0,22)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,6,0,6)}
                setOnClickListener{
                    menuDropdown.visibility = View.GONE; menuVisible = false
                    menuButton.text = "⚙️ SETTINGS MENU ▼"; action()
                }
            }
        }

        // 3 BUTTONS WELL ARRANGED WITH COLORS
        menuDropdown.addView(dropBtn("🧾 RECEIPT SETTINGS - Logo & Shop Name","#2563EB"){ showTab("RECEIPT") })
        menuDropdown.addView(dropBtn("🖨️ PRINTERS - Tax & VAT & Test Print","#16A34A"){ showTab("PRINTERS") })
        menuDropdown.addView(dropBtn("⚙️ OTHER SETTINGS - Currency & Backup","#334155"){ showTab("OTHER") })

        topMenuBox.addView(menuDropdown)
        root.addView(topMenuBox)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        val scrollContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,0,40) }

        val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        logoBase64 = pref.getString("shop_logo","") ?: ""

        // ================= RECEIPT SECTION =================
        receiptSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
            elevation=2f
        }
        receiptSection.addView(TextView(this).apply {
            text = "🏪 Shop Information - Customer's Name & Logo"; textSize = 14f
            setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); setPadding(0,0,0,12)
        })

        receiptSection.addView(TextView(this).apply {
            text = "Shop Logo (Customer's Logo)"; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,0,0,6)
        })
        logoImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(120,120).apply{gravity = Gravity.CENTER}
            setBackgroundColor(Color.parseColor("#F1F5F9"))
            scaleType = ImageView.ScaleType.CENTER_CROP
            try{
                if(logoBase64.isNotEmpty()){
                    val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                    setImageBitmap(bmp)
                }
            }catch(_:Exception){}
        }
        receiptSection.addView(logoImage)

        receiptSection.addView(Button(this).apply {
            text = "PICK LOGO FROM GALLERY"; textSize = 11f
            setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}
            setPadding(0,16,0,16)
            setOnClickListener{
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_LOGO)
            }
        })

        receiptSection.addView(Button(this).apply {
            text = "REMOVE LOGO"; textSize = 10f
            setBackgroundColor(Color.parseColor("#E2E8F0")); setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,16)}
            setPadding(0,12,0,12)
            setOnClickListener{
                logoBase64 = ""
                pref.edit().putString("shop_logo","").apply()
                logoImage.setImageResource(android.R.drawable.ic_menu_gallery)
                Toast.makeText(this@SystemSettingsActivity,"Logo removed", Toast.LENGTH_SHORT).show()
                updatePreview()
            }
        })

        fun addField(label:String, key:String, def:String): EditText {
            receiptSection.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,12,0,4) })
            val et = EditText(this).apply {
                setText(pref.getString(key, def)); hint = def
                setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(16,14,16,14); textSize = 14f
            }
            et.setOnFocusChangeListener{_, hasFocus ->
                if(!hasFocus){
                    getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString(key, et.text.toString()).apply()
                    updatePreview()
                }
            }
            receiptSection.addView(et)
            return et
        }

        val nameEt = addField("Shop Name - Customer's Name","shop_name","ruvheneko")
        val addressEt = addField("Address","shop_address","Karoi, +263773996905")
        val phoneEt = addField("Phone","shop_phone","+263773996805")
        addField("Currency","currency","$")
        addField("Receipt Footer","receipt_footer","Thank you! Come again")

        receiptSection.addView(TextView(this).apply {
            text = "RECEIPT PREVIEW"; textSize = 12f
            setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#16A34A")); setPadding(0,20,0,8)
        })

        receiptPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(16,16,16,16)
            elevation=2f
        }
        receiptSection.addView(receiptPreview)

        val watcher = object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?){ updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, st:Int, c:Int, a:Int){}
            override fun onTextChanged(s: CharSequence?, st:Int, b:Int, c:Int){}
        }
        nameEt.addTextChangedListener(watcher)
        addressEt.addTextChangedListener(watcher)
        phoneEt.addTextChangedListener(watcher)

        scrollContent.addView(receiptSection)

        // ================= PRINTERS SECTION - ADDED FROM MY CODE =================
        printerSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
            visibility = View.GONE; elevation=2f
        }
        printerSection.addView(TextView(this).apply { text = "🖨️ Printer & Tax Settings"; textSize = 14f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,12) })

        // TAX INCLUSIVE TOGGLE
        val taxRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,8,0,8); gravity=Gravity.CENTER_VERTICAL }
        taxRow.addView(TextView(this).apply{ text="Tax Inclusive"; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val switchTax = Switch(this)
        switchTax.isChecked=pref.getBoolean("tax_inclusive", true)
        switchTax.setOnCheckedChangeListener{ _, checked ->
            pref.edit().putBoolean("tax_inclusive", checked).apply()
            Toast.makeText(this, if(checked) "Tax Inclusive ON" else "Tax Exclusive", Toast.LENGTH_SHORT).show()
            updatePreview()
        }
        taxRow.addView(switchTax)
        printerSection.addView(taxRow)

        // VAT %
        val vatRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,12,0,8); gravity=Gravity.CENTER_VERTICAL }
        vatRow.addView(TextView(this).apply{ text="VAT %"; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val etVat = EditText(this).apply {
            setText(pref.getString("vat_percent","15")); layoutParams=LinearLayout.LayoutParams(120,-2)
            setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(16,12,16,12)
        }
        vatRow.addView(etVat)
        printerSection.addView(vatRow)

        // PRINTER SELECTOR
        printerSection.addView(TextView(this).apply{ text="Printer Type"; textSize=12f; setTextColor(Color.GRAY); setPadding(0,16,0,4) })
        val spinnerPrint = Spinner(this)
        spinnerPrint.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("No Printer","Bluetooth Printer","USB Printer","Network Printer"))
        printerSection.addView(spinnerPrint, LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,12) })

        // TEST PRINT BUTTON - ARRANGED
        printerSection.addView(Button(this).apply {
            text = "🖨️ TEST PRINT"; textSize=13f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            setPadding(0,20,0,20)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}
            setOnClickListener{
                val shopName = pref.getString("shop_name","ruvheneko")
                Toast.makeText(this@SystemSettingsActivity,"🖨️ Printing...\n$shopName\nTax: ${if(switchTax.isChecked) "Inclusive" else "Exclusive"}\nVAT: ${etVat.text}%\nPrinter: ${spinnerPrint.selectedItem}", Toast.LENGTH_LONG).show()
            }
        })

        // SAVE SETTINGS BUTTON - ARRANGED
        printerSection.addView(Button(this).apply {
            text = "💾 SAVE PRINTER SETTINGS"; textSize=13f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            setPadding(0,20,0,20)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,0)}
            setOnClickListener{
                pref.edit().putString("vat_percent", etVat.text.toString()).apply()
                Toast.makeText(this@SystemSettingsActivity,"Printer Settings Saved ✓", Toast.LENGTH_SHORT).show()
            }
        })

        scrollContent.addView(printerSection)

        // ================= OTHER SETTINGS =================
        otherSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
            visibility = View.GONE; elevation=2f
        }
        otherSection.addView(TextView(this).apply { text = "⚙️ Other Settings"; textSize = 14f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,12) })

        fun otherField(label:String, key:String, def:String){
            otherSection.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,12,0,4) })
            val et = EditText(this).apply {
                setText(pref.getString(key, def)); setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(16,14,16,14)
            }
            et.setOnFocusChangeListener{_, f-> if(!f) pref.edit().putString(key, et.text.toString()).apply() }
            otherSection.addView(et)
        }
        otherField("Default Currency","currency","$")
        otherField("Backup Email","backup_email","")
        otherSection.addView(Button(this).apply {
            text="BACKUP NOW"; setBackgroundColor(Color.parseColor("#334155")); setTextColor(Color.WHITE)
            setPadding(0,18,0,18); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,16,0,0)}
            setOnClickListener{ Toast.makeText(this@SystemSettingsActivity,"Backup started", Toast.LENGTH_SHORT).show() }
        })

        scrollContent.addView(otherSection)

        scroll.addView(scrollContent)
        root.addView(scroll)
        setContentView(root)

        showTab("RECEIPT")
        updatePreview()
    }

    private fun updatePreview(){
        try{
            val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val name = pref.getString("shop_name","ruvheneko") ?: "ruvheneko"
            val address = pref.getString("shop_address","Karoi") ?: "Karoi"
            val phone = pref.getString("shop_phone","+263") ?: "+263"
            val currency = pref.getString("currency","$") ?: "$"
            val footer = pref.getString("receipt_footer","Thank you!") ?: "Thank you!"
            val taxInc = pref.getBoolean("tax_inclusive", true)
            val vat = pref.getString("vat_percent","15") ?: "15"

            receiptPreview.removeAllViews()

            if(logoBase64.isNotEmpty()){
                try{
                    val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                    val iv = ImageView(this).apply {
                        setImageBitmap(bmp)
                        layoutParams = LinearLayout.LayoutParams(80,80).apply{gravity = Gravity.CENTER}
                    }
                    receiptPreview.addView(iv)
                }catch(_:Exception){}
            }

            fun txt(t:String, bold:Boolean=false, size:Float=12f, color:String="#000000"): TextView {
                return TextView(this).apply {
                    text = t; textSize = size
                    if(bold) setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor(color)); gravity = Gravity.CENTER
                    setPadding(0,2,0,2)
                }
            }
            receiptPreview.addView(txt(name, true, 16f, "#0F172A"))
            receiptPreview.addView(txt(address, false, 10f, "#64748B"))
            receiptPreview.addView(txt(phone, false, 10f, "#64748B"))
            receiptPreview.addView(txt(if(taxInc) "TAX INCLUSIVE - VAT $vat%" else "TAX EXCLUSIVE - VAT $vat%", false, 9f, "#DC2626"))
            receiptPreview.addView(txt("TOTAL: ${currency}10.00", true, 13f))
            receiptPreview.addView(txt(footer, false, 10f, "#16A34A"))
        }catch(e:Exception){}
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                logoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("shop_logo", logoBase64).apply()
                logoImage.setImageBitmap(bitmap)
                Toast.makeText(this,"Logo saved!", Toast.LENGTH_SHORT).show()
                updatePreview()
            }catch(e:Exception){
                Toast.makeText(this,"Logo error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTab(tab:String){
        receiptSection.visibility = if(tab=="RECEIPT") View.VISIBLE else View.GONE
        printerSection.visibility = if(tab=="PRINTERS") View.VISIBLE else View.GONE
        otherSection.visibility = if(tab=="OTHER") View.VISIBLE else View.GONE
    }
}