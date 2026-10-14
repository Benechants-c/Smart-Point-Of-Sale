package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
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
            text="← BACK"; setBackgroundColor(Color.parseColor("#E2E8F0"))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,0)}
        }
        back.setOnClickListener{ finish() }; root.addView(back)

        val sysPref = getSharedPreferences("system_settings", Context.MODE_PRIVATE)
        val receiptPref = getSharedPreferences("receipt_settings", Context.MODE_PRIVATE)
        sysPref.edit().putString("tax","0").apply()
        receiptPref.edit().putBoolean("show_tax",false).apply()
        val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        fun title(t:String){ root.addView(TextView(this).apply{text=t; textSize=16f; setTypeface(null,Typeface.BOLD); setPadding(20,24,20,8)} ) }
        fun label(t:String){ root.addView(TextView(this).apply{text=t; textSize=12f; setTextColor(Color.GRAY); setPadding(20,8,20,2)} ) }
        fun input(def:String, hint:String): EditText {
            val ed = EditText(this).apply{
                setText(def); this.hint=hint; setBackgroundColor(Color.WHITE); setPadding(20,16,20,16)
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,8)}
            }
            root.addView(ed); return ed
        }
        fun btn(text:String, color:String, action:()->Unit){
            val b = Button(this).apply{
                this.text=text; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE)
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,8,16,8)}
            }
            b.setOnClickListener{ action() }; root.addView(b)
        }

        title("🏪 Shop Information")
        label("Shop Name")
        val edShop = input(sysPref.getString("shop_name","Main Shop")!!, "Main Shop")
        label("Address")
        val edAddr = input(sysPref.getString("address","Harare")!!, "Address")
        label("Phone")
        val edPhone = input(sysPref.getString("phone","+263")!!, "Phone")

        title("💰 Currency - TAX INCLUSIVE")
        label("Currency ($, USD, R)")
        val edCurr = input(sysPref.getString("currency","$")!!, "$")
        root.addView(TextView(this).apply{
            text="✅ All prices TAX INCLUSIVE - No extra tax"; setBackgroundColor(Color.parseColor("#BBF7D0")); setPadding(16,12,16,12)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,8)}
        })

        title("🖨️ Printer")
        val edPrinter = input(sysPref.getString("printer_name","No Printer")!!, "Printer")
        btn("🔍 SEARCH BLUETOOTH PRINTERS","#2563EB"){
            try{
                val bt = BluetoothAdapter.getDefaultAdapter()
                if(bt==null){ Toast.makeText(this,"No Bluetooth",0).show(); return@btn }
                if(!bt.isEnabled){ Toast.makeText(this,"Enable Bluetooth",0).show(); return@btn }
                val paired = bt.bondedDevices.toList()
                if(paired.isEmpty()){ Toast.makeText(this,"No paired printers",1).show(); return@btn }
                val names = paired.map{ it.name }.toTypedArray()
                AlertDialog.Builder(this).setTitle("Select Printer").setItems(names){_,which->
                    edPrinter.setText(paired[which].name)
                    sysPref.edit().putString("printer_name",paired[which].name).apply()
                    Toast.makeText(this,"Saved ${paired[which].name}",0).show()
                }.show()
            }catch(e:Exception){ Toast.makeText(this,e.message,1).show() }
        }

        title("🧾 Receipt Layout")
        val edHeader = input(receiptPref.getString("header","*** THANK YOU ***")!!, "Header")
        val edFooter = input(receiptPref.getString("footer","Come Again!")!!, "Footer")

        btn("👁️ PREVIEW RECEIPT - TAX INCLUSIVE","#7C3AED"){
            val cur = edCurr.text.toString()
            val preview = "${edShop.text}\n${edAddr.text}\n${edHeader.text}\n----------------\nBread ${cur}2.00\nTOTAL: ${cur}2.00\n(Tax Inclusive)\n----------------\n${edFooter.text}\nDate: $autoDate"
            val tv = TextView(this).apply{text=preview; setPadding(20,20,20,20); typeface=Typeface.MONOSPACE}
            AlertDialog.Builder(this).setTitle("Receipt Preview").setView(tv).setPositiveButton("OK",null).show()
        }

        title("💾 Backup")
        btn("📤 BACKUP NOW","#16A34A"){
            try{
                var count=0
                val allData = StringBuilder()
                allData.append("Backup $autoDate\n")
                for(name in listOf("stock_main","sales_main","system_settings","receipt_settings")){
                    val p=getSharedPreferences(name,0); count+=p.all.size; allData.append("$name:${p.all.size}\n")
                }
                openFileOutput("smartpos_backup.txt",0).use{ it.write(allData.toString().toByteArray()) }
                Toast.makeText(this,"✅ BACKUP SAVED $count records",1).show()
            }catch(e:Exception){ Toast.makeText(this,"Error ${e.message}",1).show() }
        }

        btn("💾 SAVE ALL SETTINGS","#16A34A"){
            sysPref.edit().putString("shop_name",edShop.text.toString()).putString("address",edAddr.text.toString()).putString("phone",edPhone.text.toString()).putString("currency",edCurr.text.toString()).putString("tax","0").putString("printer_name",edPrinter.text.toString()).apply()
            receiptPref.edit().putString("header",edHeader.text.toString()).putString("footer",edFooter.text.toString()).putBoolean("show_tax",false).apply()
            Toast.makeText(this,"✅ SAVED ${edShop.text} | ${edCurr.text} Tax Incl",1).show()
        }

        root.addView(TextView(this).apply{text="\n$autoDate\nTAX INCLUSIVE MODE\n"; gravity=Gravity.CENTER; setPadding(20,20,20,40); setTextColor(Color.GRAY)})
        scroll.addView(root); setContentView(scroll)
    }
}