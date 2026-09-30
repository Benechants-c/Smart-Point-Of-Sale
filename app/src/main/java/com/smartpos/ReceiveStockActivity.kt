package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.text.InputType
import android.app.AlertDialog

class ReceiveStockActivity : Activity() {
    data class Line(val name:String, val code:String, val cost:Double, val sell:Double, val qty:Int)
    private val list = mutableListOf<Line>()

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE)}
        root.addView(TextView(this).apply{text="RECEIVE STOCK - NEW (BUILD 113)";textSize=18f;setTextColor(Color.BLACK);setPadding(0,0,0,20)})

        // Supplier + Date Row
        val topRow = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val supplier = Spinner(this).apply{adapter=ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_spinner_item, listOf("Optional - Select Supplier","Supplier A"))}
        topRow.addView(supplier, LinearLayout.LayoutParams(0,-2,1f))
        val date = TextView(this).apply{text=java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date());gravity=Gravity.END}
        topRow.addView(date, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(topRow)

        val invRow = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,20,0,0)}
        invRow.addView(TextView(this).apply{text="Invoice No\n[Optional]";layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        val inv = TextView(this).apply{text="AUTO-000125";gravity=Gravity.END;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        invRow.addView(inv)
        root.addView(invRow)

        root.addView(TextView(this).apply{text="Product | Code | Cost | Sell | Qty";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(20,12,20,12);setPadding(0,20,0,0)})

        val table = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F8FAFC"))}
        fun refresh(){
            table.removeAllViews()
            // Demo existing - keep your existing products loading logic here if needed
            list.forEach{
                table.addView(TextView(this).apply{text="${it.name} | ${it.code} | ${it.cost} | ${it.sell} | ${it.qty}";setPadding(12,12,12,12)})
            }
            var tq=0;var tc=0.0
            list.forEach{tq+=it.qty; tc+=it.cost*it.qty}
            total.text="Total: ${list.size} Products, Qty: $tq COST: $${"%.1f".format(tc)}"
        }

        // Pre-fill demo if empty (remove this in production)
        if(list.isEmpty()){
            list.add(Line("Coca-Cola 500ml","12345",0.6,1.0,24))
            list.add(Line("Mazoe Orange","67890",2.5,3.5,12))
            list.add(Line("Bread 700g","45678",1.2,1.8,20))
        }

        root.addView(table)
        val addBtn = Button(this).apply{text="ADD PRODUCT";setBackgroundColor(Color.parseColor("#E5E7EB"))}
        root.addView(addBtn)

        val total = TextView(this).apply{text="Total: 0 Products, Qty: 0 COST: $0.0";setBackgroundColor(Color.parseColor("#DBEAFE"));gravity=Gravity.CENTER;setPadding(20,12,20,12)}
        root.addView(total)

        val bottom = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        bottom.addView(Button(this).apply{text="SAVE DRAFT";layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        bottom.addView(Button(this).apply{text="PRINT GRN";layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        bottom.addView(Button(this).apply{text="COMPLETE\nRECEIVE";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        root.addView(bottom)

        refresh()

        addBtn.setOnClickListener{
            val dlgView = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,20,30,20)}
            val nameIn = EditText(this).apply{hint="Product Name *"}
            val codeIn = EditText(this).apply{hint="Code * (Required)"}
            val costIn = EditText(this).apply{hint="Cost * (Required) e.g. 5.00";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
            val sellIn = EditText(this).apply{hint="Selling * (Required) e.g. 8.00";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
            val qtyIn = EditText(this).apply{hint="Quantity * (Required) e.g. 10";inputType=InputType.TYPE_CLASS_NUMBER}
            val err = TextView(this).apply{setTextColor(Color.RED);textSize=12f}
            dlgView.addView(nameIn);dlgView.addView(codeIn);dlgView.addView(costIn);dlgView.addView(sellIn);dlgView.addView(qtyIn);dlgView.addView(err)

            val dialog = AlertDialog.Builder(this).setTitle("Add Product (BUILD 113)").setView(dlgView)
               .setPositiveButton("ADD",null).setNegativeButton("CANCEL",null).create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                val n = nameIn.text.toString().trim()
                val c = codeIn.text.toString().trim()
                val coStr = costIn.text.toString().trim()
                val seStr = sellIn.text.toString().trim()
                val qStr = qtyIn.text.toString().trim()
                if(n.isEmpty()){err.text="❌ Product Name required!";return@setOnClickListener}
                if(c.isEmpty()){err.text="❌ Code required! Can't be empty";return@setOnClickListener}
                if(coStr.isEmpty()){err.text="❌ Cost required!";return@setOnClickListener}
                if(seStr.isEmpty()){err.text="❌ Selling price required!";return@setOnClickListener}
                if(qStr.isEmpty()){err.text="❌ Quantity required!";return@setOnClickListener}
                val co = coStr.toDoubleOrNull(); if(co==null||co<=0){err.text="❌ Cost must be >0";return@setOnClickListener}
                val se = seStr.toDoubleOrNull(); if(se==null||se<=0){err.text="❌ Selling must be >0";return@setOnClickListener}
                val q = qStr.toIntOrNull(); if(q==null||q<=0){err.text="❌ Qty must be >0 integer";return@setOnClickListener}
                list.add(Line(n,c,co,se,q))
                refresh()
                dialog.dismiss()
                Toast.makeText(this,"✅ $n Added",Toast.LENGTH_SHORT).show()
            }
        }
        setContentView(ScrollView(this).apply{addView(root)})
    }
}
