package com.smartpos.app

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

        // HEADER
        root.addView(TextView(this).apply {
            text = "RECEIVE STOCK [+ NEW]"
            textSize = 20f
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0,0,0,20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        // ROW 1: Supplier (OPTIONAL) + Date
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        supplierSpinner = Spinner(this)
        val suppliers = arrayOf("Optional - Select Supplier", "ABC Suppliers", "Delta Beverages", "Baker's Inn", "Walk-in")
        supplierSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suppliers)

        val dateInput = EditText(this).apply { setText("29/09/2026") }
        row1.addView(supplierSpinner, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0,0,8,0) })
        row1.addView(dateInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(8,0,0,0) })
        root.addView(row1)

        // ROW 2: Invoice (OPTIONAL) + GRN AUTO
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        invoiceInput = EditText(this).apply { hint = "Invoice No [Optional]" }
        grnInput = EditText(this).apply {
            setText("AUTO-000125")
            isEnabled = false
            setBackgroundColor(Color.parseColor("#D1D5DB"))
        }
        row2.addView(invoiceInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0,8,8,0) })
        row2.addView(grnInput, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(8,8,0,0) })
        root.addView(row2)

        // TABLE HEADER
        root.addView(TextView(this).apply {
            text = " Product Name Code Cost Selling Quantity"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setPadding(12,18,12,18)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        // TABLE CONTAINER
        tableContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(tableContainer)

        // DEMO DATA
        items.add(ReceiveItem("Coca-Cola 500ml", "12345", 0.60, 1.00, 24))
        items.add(ReceiveItem("Mazoe Orange", "67890", 2.50, 3.50, 12))
        items.add(ReceiveItem("Bread 700g", "45678", 1.20, 1.80, 20))
        refreshTable()

        // + ADD PRODUCT
        val addBtn = Button(this).apply {
            text = "+ ADD PRODUCT"
            setBackgroundColor(Color.WHITE)
            setTextColor(Color.parseColor("#2563EB"))
        }
        addBtn.setOnClickListener { showAddDialog() }
        root.addView(addBtn)

        // TOTALS
        totalText = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#DBEAFE"))
            setPadding(16,20,16,20)
            gravity = Gravity.CENTER
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(totalText)
        updateTotals()

        // ACTION BUTTONS
        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnDraft = Button(this).apply { text = "SAVE DRAFT"; setBackgroundColor(Color.parseColor("#E5E7EB")) }
        val btnPrint = Button(this).apply { text = "PRINT GRN"; setBackgroundColor(Color.parseColor("#E5E7EB")) }
        val btnComplete = Button(this).apply {
            text = "COMPLETE RECEIVE"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
        }

        btnComplete.setOnClickListener {
            // OPTIONAL LOGIC HERE - NO VALIDATION FOR SUPPLIER/INVOICE
            val supplier = if (supplierSpinner.selectedItemPosition == 0) "No Supplier / Walk-in" else supplierSpinner.selectedItem.toString()
            val invoice = if (invoiceInput.text.toString().trim().isEmpty()) "NO-INV-${System.currentTimeMillis()}" else invoiceInput.text.toString()
            val grn = grnInput.text.toString()

            // TODO: Save to DB - update stock
            Toast.makeText(this, "Stock Received!\nSupplier: $supplier\nInvoice: $invoice\nGRN: $grn\nTotal Cost: $${calcTotalCost()}", Toast.LENGTH_LONG).show()
            // After saving, clear or go back
            // finish()
        }

        btnDraft.setOnClickListener { Toast.makeText(this, "Draft Saved", Toast.LENGTH_SHORT).show() }
        btnPrint.setOnClickListener { Toast.makeText(this, "Printing GRN: ${grnInput.text}", Toast.LENGTH_SHORT).show() }

        actionRow.addView(btnDraft, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)})
        actionRow.addView(btnPrint, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)})
        actionRow.addView(btnComplete, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)})
        root.addView(actionRow)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun refreshTable() {
        tableContainer.removeAllViews()
        for (item in items) {
            val row = TextView(this).apply {
                text = "${item.name.padEnd(18)} ${item.code} ${item.cost} ${item.selling} ${item.qty}"
                setPadding(12,20,12,20)
                setBackgroundColor(Color.WHITE)
            }
            tableContainer.addView(row)
        }
    }

    private fun updateTotals() {
        val totalQty = items.sumOf { it.qty }
        totalText.text = "Total Products: ${items.size} Total Quantity: $totalQty TOTAL COST: $${calcTotalCost()}"
    }

    private fun calcTotalCost(): Double {
        return items.sumOf { it.cost * it.qty }
    }

    private fun showAddDialog() {
        val dialogView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val nameI = EditText(this).apply { hint = "Product Name" }
        val codeI = EditText(this).apply { hint = "Code" }
        val costI = EditText(this).apply { hint = "Cost"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val sellI = EditText(this).apply { hint = "Selling"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val qtyI = EditText(this).apply { hint = "
