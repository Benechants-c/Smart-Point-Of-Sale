package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16)
        }
        header.addView(TextView(this).apply {
            text = "⚙️ System Settings - TAX INCLUSIVE"
            textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        })
        root.addView(header)

        root.addView(Button(this).apply {
            text = "BACK"; textSize = 12f
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
            setOnClickListener{ finish() }
        })

        val topMenuBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(12,8,12,8)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)}
        }
        val menuBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        menuButton = Button(this).apply {
            text = "⚙️ SETTINGS MENU ▼"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE)
            setPadding(24,14,24,14)
            setOnClickListener{
                menuVisible = !menuVisible
                menuDropdown.visibility = if(menuVisible) View.VISIBLE else View.GONE
                text = if(menuVisible) "⚙️ SETTINGS MENU ▲" else "⚙️ SETTINGS MENU ▼"
            }
        }
        menuBar.addView(menuButton, LinearLayout.LayoutParams(-1,-2))
        topMenuBox.addView(menuBar)

        menuDropdown = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE
            setPadding(8,8,8,8); setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        fun dropBtn(title:String, color:String, action:()->Unit): Button {
            return Button(this).apply {
                text = title; textSize = 12f; setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE)
                setPadding(16,18,16,18)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,4,0,4)}
                setOnClickListener{
                    menuDropdown.visibility = View.GONE; menuVisible = false
                    menuButton.text = "⚙️ SETTINGS MENU ▼"; action()
                }
            }
        }
        menuDropdown.addView(dropBtn("🧾 RECEIPT SETTINGS","#2563EB"){ showTab("RECEIPT") })
        menuDropdown.addView(dropBtn("🖨️ PRINTERS","#16A34A"){ showTab("PRINTERS") })
        menuDropdown.addView(dropBtn("⚙️ OTHER SETTINGS","#334155"){ showTab("OTHER") })
        topMenuBox.addView(menuDropdown)
        root.addView(topMenuBox)

        val scroll = ScrollView(this)
        val scrollContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,0,40) }

        val pref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        logoBase64 = pref.getString("shop_logo","") ?: ""

        receiptSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
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
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,12)}
            setOnClickListener{
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_LOGO)
            }
        })

        receiptSection.addView(Button(this).apply {
            text = "REMOVE LOGO"; textSize = 10f
            setBackgroundColor(Color.parseColor("#E2E8F0")); setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,16)}
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
        val currencyEt = addField("Currency","currency","$")
        val footerEt = addField("Receipt Footer","receipt_footer","Thank you! Come again")

        receiptSection.addView(TextView(this).apply {
            text = "RECEIPT PREVIEW"; textSize = 12f
            setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#16A34A")); setPadding(0,20,0,8)
        })

        receiptPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(16,16,16,16)
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

        printerSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
            visibility = View.GONE
        }
        printerSection.addView(TextView(this).apply { text = "Printers"; textSize = 14f; setTypeface(null, Typeface.BOLD) })
        printerSection.addView(Button(this).apply {
            text = "TEST PRINT"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,16,0,0)}
            setOnClickListener{ Toast.makeText(this@SystemSettingsActivity,"Test print", Toast.LENGTH_SHORT).show() }
        })
        scrollContent.addView(printerSection)

        otherSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,8)}
            visibility = View.GONE
        }
        otherSection.addView(TextView(this).apply { text = "Other Settings"; textSize = 14f; setTypeface(null, Typeface.BOLD) })
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