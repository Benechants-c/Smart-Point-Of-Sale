package com.smartpos

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog

data class ReceiveItem(var name:String, var code:String, var cost:Double, var selling:Double, var qty:Int)

class ReceiveStockActivity : AppCompatActivity() {

    private val items = mutableListOf<ReceiveItem>()
    private lateinit var tableContainer: LinearLayout
    private lateinit var totalText: TextView
    private lateinit var supplierSpinner: Spinner
    private lateinit var invoiceInput: EditText
    private lateinit var grnInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,24,24,24)
            setBackgroundColor(Color.parseColor("#F5F7FB"))
        }

        root.addView(TextView(this).apply {
            text = "RECEIVE STOCK - NEW"
            textSize = 20f
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0,0,0,20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        supplierSpinner = Spinner(this)
        val suppliers = arrayOf("Optional - Select Supplier", "ABC Suppliers", "Delta Beverages", "Bakers Inn", "Walk-in")
        supplierSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suppliers)
        val dateInput = EditText(this).apply { setText("29/09/2026") }
        row1.addView(supplierSpinner, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0,0,8,0) })
        row1.addView(dateInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(8,0,0,0) })
        root.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        invoiceInput = EditText(this).apply { hint = "Invoice No [Optional]" }
        grnInput = EditText(this).apply {
            setText("AUTO-000125")
            isEnabled = false
        }
        row2.addView(invoiceInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0,8,8,0) })
        row2.addView(grnInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(8,8,0,0) })
        root.addView(row2)

        root.addView(TextView(this).apply {
            text = " Product | Code | Cost | Sell | Qty"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setPadding(12,18,12,18)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        tableContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(tableContainer)

        items.add(ReceiveItem("Coca-Cola 500ml", "12345", 0.60, 1.00, 24))
        items.add(ReceiveItem("Mazoe Orange", "67890", 2.50, 3.50, 12))
        items.add(ReceiveItem("Bread 700g", "45678", 1.20, 1.80, 20))
        refreshTable()

        val addBtn = Button(this).apply { text = "ADD PRODUCT" }
        addBtn.setOnClickListener { showAddDialog() }
        root.addView(addBtn)

        totalText = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#DBEAFE"))
            setPadding(16,20,16,20)
            gravity = Gravity.CENTER
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(totalText)
        updateTotals()

        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnDraft = Button(this).apply { text = "SAVE DRAFT" }
        val btnPrint = Button(this).apply { text = "PRINT GRN" }
        val btnComplete = Button(this).apply {
            text = "COMPLETE RECEIVE"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
        }

        btnComplete.setOnClickListener {
            val supplier = if (supplierSpinner.selectedItemPosition == 0) "No Supplier" else supplierSpinner.selectedItem.toString()
            val invoice = if (invoiceInput.text.toString().trim().isEmpty()) "NO-INV" else invoiceInput.text.toString()
            Toast.makeText(this, "Stock Received! Supplier: " + supplier + " Invoice: " + invoice, Toast.LENGTH_LONG).show()
        }

        actionRow.addView(btnDraft, LinearLayout.LayoutParams(0,-2,1f))
        actionRow.addView(btnPrint, LinearLayout.LayoutParams(0,-2,1f))
        actionRow.addView(btnComplete, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(actionRow)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun refreshTable() {
        tableContainer.removeAllViews()
        for (item in items) {
            val row = TextView(this).apply {
                text = item.name + " | " + item.code + " | " + item.cost + " | " + item.selling + " | " + item.qty
                setPadding(12,20,12,20)
                setBackgroundColor(Color.WHITE)
            }
            tableContainer.addView(row)
        }
    }

    private fun updateTotals() {
        var totalQty = 0
        var totalCost = 0.0
        for (it in items) {
            totalQty += it.qty
            totalCost += it.cost * it.qty
        }
        totalText.text = "Total: " + items.size + " Products, Qty: " + totalQty + " COST: $" + totalCost
    }

    private fun showAddDialog() {
        val dialogView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val nameI = EditText(this).apply { hint = "Product Name" }
        val codeI = EditText(this).apply { hint = "Code" }
        val costI = EditText(this).apply { hint = "Cost" }
        val sellI = EditText(this).apply { hint = "Selling" }
        val qtyI = EditText(this).apply { hint = "Quantity" }
        dialogView.addView(nameI); dialogView.addView(codeI); dialogView.addView(costI); dialogView.addView(sellI); dialogView.addView(qtyI)

        AlertDialog.Builder(this)
          .setTitle("Add Product")
          .setView(dialogView)
          .setPositiveButton("Add") { _, _ ->
                val name = nameI.text.toString()
                if (name.isNotEmpty()) {
                    items.add(ReceiveItem(name, codeI.text.toString(), costI.text.toString().toDoubleOrNull()?:0.0, sellI.text.toString().toDoubleOrNull()?:0.0, qtyI.text.toString().toIntOrNull()?:1))
                    refreshTable(); updateTotals()
                }
            }
          .setNegativeButton("Cancel", null)
          .show()
    }
}
