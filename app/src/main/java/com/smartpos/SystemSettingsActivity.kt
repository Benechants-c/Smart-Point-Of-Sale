package com.smartpos

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SystemSettingsActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val header = TextView(this).apply{
            text="⚙️ System Settings - TAX INCLUSIVE"; textSize=18f; setTypeface(null,Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,30,20,24)
        }
        root.addView(header)

        val back = Button(this).apply{
            text="← BACK"; textSize=14f; setBackgroundColor(Color.parseColor("#E2E8F0")); setTextColor(Color.BLACK)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,0)}
        }
        back.setOnClickListener{ finish() }; root.addView(back)

        val sysPref = getSharedPreferences("system_settings", Context.MODE_PRIVATE)
        val receiptPref = getSharedPreferences("receipt_settings", Context.MODE_PRIVATE)
        // Force tax inclusive - no tax
        sysPref.edit().putString("tax","0").apply()
        receiptPref.edit().putBoolean("show_tax",false).apply()

        val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        fun title(t:String){
            root.addView(TextView(this).apply{
                text=t; textSize=16f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#0F172A"))
                setPadding(20,24,20,8)
            })
        }
        fun label(t:String){
            root.addView(TextView(this).apply{
                text=t; textSize=13f; setTextColor(Color.GRAY); setPadding(20,8,20,2)
            })
        }
        fun input(def:String, hint:String): EditText {
            val ed = EditText(this).apply{
                setText(def); this.hint=hint; textSize=15f; setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE); setPadding(20,16,20,16)
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,8)}
            }
            root.addView(ed); return ed
        }
        fun btn(text:String, color:String, action:()->Unit): Button {
            val b = Button(this).apply{
                this.text=text; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE); textSize=14f
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,8,16,8)}
            }
            b.setOnClickListener{ action() }; root.addView(b); return b
        }
        fun switchRow(text:String, def:Boolean, prefKey:String): CheckBox {
            val cb = CheckBox(this).apply{
                this.text=text; isChecked=def; textSize=14f
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,4)}
                setOnCheckedChangeListener{_,v-> receiptPref.edit().putBoolean(prefKey,v).apply() }
            }
            root.addView(cb); return cb
        }

        // 1. SHOP INFO
        title("🏪 Shop Information - REAL")
        label("Shop Name")
        val edShop = input(sysPref.getString("shop_name","Main Shop")!!, "Main Shop")
        label("Address")
        val edAddr = input(sysPref.getString("address","Harare, Zimbabwe")!!, "Address")
        label("Phone / WhatsApp")
        val edPhone = input(sysPref.getString("phone","+263")!!, "Phone")

        // 2. CURRENCY - TAX REMOVED
        title("💰 Currency - TAX INCLUSIVE (No Tax)")
        label("Currency Symbol ($, USD, R, P)")
        val edCurr = input(sysPref.getString("currency","$")!!, "$")
        root.addView(TextView(this).apply{
            text="✅ All prices are TAX INCLUSIVE - No extra tax will be added"; textSize=12f; setTextColor(Color.parseColor("#16A34A"))
            setTypeface(null,Typeface.BOLD); setPadding(20,4,20,8); setBackgroundColor(Color.parseColor("#BBF7D0"))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,8)}
        })

        // 3. PRINTER
        title("🖨️ Printer Settings - REAL BLUETOOTH")
        label("Printer Name")
        val edPrinter = input(sysPref.getString("printer_name","No Printer")!!, "Printer Name")
        btn("🔍 SEARCH BLUETOOTH PRINTERS","#2563EB"){
            try{
                val bt = BluetoothAdapter.getDefaultAdapter()
                if(bt==null){ Toast.makeText(this,"No Bluetooth",0).show(); return@btn }
                if(!bt.isEnabled){ Toast.makeText(this,"Enable Bluetooth first",0).show(); return@btn }
                val paired = bt.bondedDevices
                if(paired.isEmpty()){ Toast.makeText(this,"No paired printers - Pair in phone settings",1).show(); return@btn }
                val names = paired.map{ "${it.name} - ${it.address}" }
                AlertDialog.Builder(this).setTitle("Select Printer").setItems(names.toTypedArray()){_,which->
                    val dev = paired.toList()[which]
                    edPrinter.setText(dev.name)
                    sysPref.edit().putString("printer_name",dev.name).putString("printer_mac",dev.address).apply()
                    Toast.makeText(this,"✅ Printer ${dev.name} saved",0).show()
                }.show()
            }catch(e:Exception){ Toast.makeText(this,"BT Error: ${e.message}",1).show() }
        }
        btn("🧾 TEST PRINT - TAX INCLUSIVE","#0D9488"){
            Toast.makeText(this,"🖨️ Test Print:\n${edShop.text}\nPrice: ${edCurr.text}10.00 (Tax Incl)\nTOTAL: ${edCurr.text}10.00",1).show()
        }

        // 4. RECEIPT LAYOUT - NO TAX
        title("🧾 Receipt Layout Editor - TAX INCLUSIVE")
        label("Receipt Header (top)")
        val edReceiptHeader = input(receiptPref.getString("header","*** THANK YOU ***")!!, "Thank you message top")
        label("Receipt Footer (bottom)")
        val edReceiptFooter = input(receiptPref.getString("footer","Come Again!")!!, "Come again")
        label("Show on Receipt")
        switchRow("Show Shop Name", receiptPref.getBoolean("show_shop",true), "show_shop")
        switchRow("Show Address", receiptPref.getBoolean("show_address",true), "show_address")
        switchRow("Show Phone", receiptPref.getBoolean("show_phone",true), "show_phone")
        switchRow("Show Date/Time", receiptPref.getBoolean("show_date",true), "show_date")
        switchRow("Show Cashier Name", receiptPref.getBoolean("show_cashier",false), "show_cashier")

        btn("👁️ PREVIEW RECEIPT - NO TAX","#7C3AED"){
            val preview = """
                ${if(receiptPref.getBoolean("show_shop",true)) edShop.text.toString() else ""}
                ${if(receiptPref.getBoolean("show_address",true)) edAddr.text.toString() else ""}
                ${if(receiptPref.getBoolean("show_phone",true)) "Tel: ${edPhone.text}" else ""}

                ${edReceiptHeader.text}

                1x Bread        ${edCurr.text}2.00
                2x Milk         ${edCurr.text}3.00

                TOTAL:          ${edCurr.text}5.00
                (All Prices Tax Inclusive)

                ${edReceiptFooter.text}
                Date: $autoDate
                ${if(receiptPref.getBoolean("show_cashier",false)) "Cashier: Admin" else ""}
            """.trimIndent()
            val tv = TextView(this).apply{ text=preview; textSize=14f; setPadding(24,24,24,24); typeface=Typeface.MONOSPACE; setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE) }
            AlertDialog.Builder(this).setTitle("Receipt - TAX INCLUSIVE").setView(tv).setPositiveButton("OK",null).show()
        }

        // 5. BACKUP & RESTORE
        title("💾 Backup & Restore - REAL")
        btn("📤 BACKUP NOW - SAVE TO FILE","#16A34A"){
            try{
                val backup = JSONObject()
                val prefsList = listOf("stock_main","products_db","products","sales_main","sales_db","customers","customers_db","system_settings","receipt_settings","cash")
                for(pName in prefsList){
                    val p = getSharedPreferences(pName,0)
                    val obj = JSONObject()
                    for((k,v) in p.all){ obj.put(k,v.toString()) }
                    backup.put(pName, obj)
                }
                backup.put("backup_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                backup.put("tax_mode","INCLUSIVE")
                openFileOutput("smartpos_backup.json",0).use{ it.write(backup.toString().toByteArray()) }
                try{
                    val dir = File(getExternalFilesDir(null),"backup"); dir.mkdirs()
                    File(dir,"smartpos_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json").writeText(backup.toString())
                }catch(_:Exception){}
                Toast.makeText(this,"✅ BACKUP SAVED! Tax Inclusive Mode",Toast.LENGTH_LONG).show()
            }catch(e:Exception){ Toast.makeText(this,"Backup Error: ${e.message}",1).show() }
        }
        btn("📥 RESTORE FROM BACKUP","#EA580C"){
            try{
                if(!fileList().contains("smartpos_backup.json")){ Toast.makeText(this,"No backup found",0).show(); return@btn }
                val content = openFileInput("smartpos_backup.json").bufferedReader().readText()
                val json = JSONObject(content)
                for(pName in json.keys()){
                    if(pName=="backup_date" || pName=="tax_mode" || pName=="app_version") continue
                    try{
                        val pObj = json.getJSONObject(pName)
                        val pref = getSharedPreferences(pName,0).edit()
                        for(k in pObj.keys()){ pref.putString(k,pObj.getString(k)) }
                        pref.apply()
                    }catch(_:Exception){}
                }
                Toast.makeText(this,"✅ RESTORE DONE! ${json.optString("backup_date")}",Toast.LENGTH_LONG).show()
            }catch(e:Exception){ Toast.makeText(this,"Restore Error: ${e.message}",1).show() }
        }

        // SAVE ALL
        title("✅ Save All Settings")
        btn("💾 SAVE ALL SETTINGS NOW","#16A34A"){
            sysPref.edit()
                .putString("shop_name",edShop.text.toString())
                .putString("address",edAddr.text.toString())
                .putString("phone",edPhone.text.toString())
                .putString("currency",edCurr.text.toString())
                .putString("tax","0")
                .putString("printer_name",edPrinter.text.toString())
                .apply()
            receiptPref.edit()
                .putString("header",edReceiptHeader.text.toString())
                .putString("footer",edReceiptFooter.text.toString())
                .putBoolean("show_tax",false)
                .apply()
            Toast.makeText(this,"✅ ALL SAVED TAX INCLUSIVE!\nShop: ${edShop.text}\nCurrency: ${edCurr.text}\nNo Tax - Inclusive",Toast.LENGTH_LONG).show()
        }

        root.addView(TextView(this).apply{
            text="\nAuto Date: $autoDate\nVersion 1.0 - TAX INCLUSIVE MODE\nAll Prices Include Tax\nNo Extra Tax Added\n"; textSize=11f; gravity=Gravity.CENTER; setPadding(20,20,20,40); setTextColor(Color.GRAY)
        })

        scroll.addView(root); setContentView(scroll)
    }
}