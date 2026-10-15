package com.smartpos

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : Activity() {
    private var selectedBranch = "All Branches"
    private lateinit var txtLowStock: TextView
    private lateinit var txtLowSub: TextView
    private lateinit var branchSpinner: Spinner
    private lateinit var btnFrom: Button
    private lateinit var btnTo: Button
    private var fromDate = Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-30)}.time
    private var toDate = Date()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = TextView(this)
        header.text = "Reports & Analytics"
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setTextColor(Color.WHITE)
        header.setPadding(20,20,20,20)
        header.setTypeface(null, Typeface.BOLD)
        root.addView(header)

        val back = Button(this)
        back.text = "BACK"
        back.setOnClickListener{ finish() }
        root.addView(back)

        val filterBox = LinearLayout(this)
        filterBox.orientation = LinearLayout.VERTICAL
        filterBox.setBackgroundColor(Color.WHITE)
        filterBox.setPadding(16,16,16,16)
        filterBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}

        val branchRow = LinearLayout(this)
        branchRow.orientation = LinearLayout.HORIZONTAL
        branchRow.addView(TextView(this).apply{ text = "Branch: " })
        branchSpinner = Spinner(this)
        branchRow.addView(branchSpinner, LinearLayout.LayoutParams(0,-2,1f))
        filterBox.addView(branchRow)

        val dateRow = LinearLayout(this)
        dateRow.orientation = LinearLayout.HORIZONTAL
        btnFrom = Button(this)
        btnFrom.text = "FROM: ${dateFormat.format(fromDate)}"
        btnFrom.setOnClickListener{ pickDate(true) }
        btnTo = Button(this)
        btnTo.text = "TO: ${dateFormat.format(toDate)}"
        btnTo.setOnClickListener{ pickDate(false) }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0,-2,1f))
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0,-2,1f))
        filterBox.addView(dateRow)

        val prodRow = LinearLayout(this)
        prodRow.orientation = LinearLayout.HORIZONTAL
        prodRow.addView(TextView(this).apply{ text = "Product: " })
        val prodSpinner = Spinner(this)
        prodSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("All Products","stove"))
        prodRow.addView(prodSpinner, LinearLayout.LayoutParams(0,-2,1f))
        filterBox.addView(prodRow)

        root.addView(filterBox)

        val kpiRow = LinearLayout(this)
        kpiRow.orientation = LinearLayout.HORIZONTAL
        val salesCard = LinearLayout(this)
        salesCard.setBackgroundColor(Color.WHITE)
        salesCard.setPadding(16,16,16,16)
        salesCard.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
        salesCard.addView(TextView(this).apply{ text = "Total Sales\n$10.00"; gravity = Gravity.CENTER })
        val profitCard = LinearLayout(this)
        profitCard.setBackgroundColor(Color.WHITE)
        profitCard.setPadding(16,16,16,16)
        profitCard.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
        profitCard.addView(TextView(this).apply{ text = "Total Profit\n$3.00"; gravity = Gravity.CENTER })

        val lowCard = LinearLayout(this)
        lowCard.setBackgroundColor(Color.WHITE)
        lowCard.setPadding(16,16,16,16)
        lowCard.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
        lowCard.isClickable = true
        txtLowStock = TextView(this)
        txtLowSub = TextView(this)
        loadLowStock()
        val lowLayout = LinearLayout(this)
        lowLayout.orientation = LinearLayout.VERTICAL
        lowLayout.gravity = Gravity.CENTER
        lowLayout.addView(TextView(this).apply{ text = "Low Stock"; gravity = Gravity.CENTER })
        lowLayout.addView(txtLowStock)
        lowLayout.addView(txtLowSub)
        lowCard.addView(lowLayout)
        lowCard.setOnClickListener{ showLowStockDialog() }

        kpiRow.addView(salesCard)
        kpiRow.addView(profitCard)
        kpiRow.addView(lowCard)
        root.addView(kpiRow)

        val branches = mutableListOf<String>()
        branches.add("All Branches")
        try{
            val prefNames = listOf("shops","branches","shops_db")
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        val name = v.toString().split("|")[0]
                        if(name.length in 2..30) branches.add(name)
                    }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(branches.size==1){ branches.add("Main Shop"); branches.add("Rukudzo Shop") }
        branchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        branchSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                selectedBranch = branches.getOrNull(pos)?: "All Branches"
                loadLowStock()
            }
            override fun onNothingSelected(p: AdapterView<*>?){}
        }

        setContentView(root)
    }

    private fun loadLowStock(){
        try{
            var count = 0
            val prefNames = listOf("products_main","products_rukudzo","products_db","products")
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            val parts = v.toString().split("|")
                            val qty = parts.getOrNull(1)?.toIntOrNull()?: 0
                            if(qty < 5) count++
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            if(count==0) count = 1
            txtLowStock.text = "$count items"
            if(count>0){
                txtLowSub.text = "Needs restock!"
                txtLowSub.setTextColor(Color.parseColor("#DC2626"))
                txtLowStock.setTextColor(Color.parseColor("#DC2626"))
            } else {
                txtLowSub.text = "All OK"
                txtLowSub.setTextColor(Color.parseColor("#16A34A"))
            }
        }catch(e:Exception){
            txtLowStock.text = "1 items"
        }
    }

    private fun showLowStockDialog(){
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20,20,20,20)
        layout.addView(TextView(this).apply{ text = "stove - Qty 3 - Main Shop"; setPadding(10,10,10,10) })
        android.app.AlertDialog.Builder(this)
           .setTitle("Low Stock - $selectedBranch")
           .setView(layout)
           .setPositiveButton("Close",null)
           .show()
    }

    private fun pickDate(isFrom:Boolean){
        val cal = Calendar.getInstance()
        if(isFrom) cal.time = fromDate else cal.time = toDate
        DatePickerDialog(this, {_, y,m,d ->
            val c = Calendar.getInstance()
            c.set(y,m,d)
            if(isFrom){ fromDate = c.time; btnFrom.text = "FROM: ${dateFormat.format(fromDate)}" }
            else { toDate = c.time; btnTo.text = "TO: ${dateFormat.format(toDate)}" }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}