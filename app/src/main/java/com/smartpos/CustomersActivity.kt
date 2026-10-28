package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

data class CustomerItem(
    val id: String,
    val name: String,
    val phone: String,
    val location: String,
    val createdDate: String
)

class CustomersActivity : Activity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText
    private lateinit var etSearch: EditText
    private lateinit var container: LinearLayout
    private lateinit var tvCount: TextView

    private val allCustomers = mutableListOf<CustomerItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#00796B"))
            setPadding(30,25,30,25)
        }
        val title = TextView(this).apply {
            text = "CUSTOMERS - ADD & MANAGE"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvTime = TextView(this).apply {
            text = "Real Time: ${sdf.format(Date())} | Device: ${android.os.Build.MODEL}"
            setTextColor(Color.parseColor("#B2DFDB"))
            textSize = 12f
        }
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier"))
        val tvCashier = TextView(this).apply {
            text = "Cashier: $cashier"
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        header.addView(title)
        header.addView(tvCashier)
        header.addView(tvTime)

        // ADD CUSTOMER FORM - Name, Phone, Location
        val addBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
        }
        val addTitle = TextView(this).apply {
            text = "Add New Customer"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00796B"))
            setPadding(0,0,0,15)
        }

        etName = EditText(this).apply {
            hint = "Name (e.g. John Moyo)"
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        etPhone = EditText(this).apply {
            hint = "Phone Number (e.g. 0771234567)"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        etLocation = EditText(this).apply {
            hint = "Location (e.g. Harare CBD, Mbare)"
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }

        val btnAdd = Button(this).apply {
            text = "SAVE CUSTOMER"
            setBackgroundColor(Color.parseColor("#00796B"))
            setTextColor(Color.WHITE)
        }

        // Spacing
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0,10,0,10)
        etName.layoutParams = lp
        etPhone.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,10,0,10) }
        etLocation.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,10,0,15) }

        addBox.addView(addTitle)
        addBox.addView(etName)
        addBox.addView(etPhone)
        addBox.addView(etLocation)
        addBox.addView(btnAdd)

        // SEARCH AT TOP
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0F2F1"))
            setPadding(15,15,15,15)
        }
        etSearch = EditText(this).apply {
            hint = "Search by Name / Phone / Location..."
            setBackgroundColor(Color.WHITE)
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        tvCount = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(5,10,5,0)
        }
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        // LIST HEADER
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#37474F"))
            setPadding(12,14,12,14)
        }
        fun addH(t: String, w: Float) {
            val tv = TextView(this@CustomersActivity).apply {
                text = t; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            }
            listHeader.addView(tv)
        }
        addH("NAME", 1.5f)
        addH("PHONE", 1.2f)
        addH("LOCATION", 1.5f)
        addH("DATE", 1f)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5,5,5,5)
        }
        scroll.addView(container)

        root.addView(header)
        root.addView(addBox)
        root.addView(searchBox)
        root.addView(listHeader)
        root.addView(scroll)

        setContentView(root)

        loadCustomers()
        render(allCustomers)

        btnAdd.setOnClickListener { saveCustomer() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) allCustomers else allCustomers.filter {
                    it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.location.lowercase().contains(q)
                }
                render(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun saveCustomer() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val location = etLocation.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Enter name"
            return
        }
        if (phone.isEmpty()) {
            etPhone.error = "Enter phone"
            return
        }
        if (location.isEmpty()) {
            etLocation.error = "Enter location"
            return
        }

        val id = "CUST_${System.currentTimeMillis()}"
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val customer = CustomerItem(id, name, phone, location, date)

        // Save to SharedPreferences - format: Name|Phone|Location|Date
        val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)
        custPrefs.edit().putString(id, "${name}|${phone}|${location}|${date}").apply()

        allCustomers.add(0, customer)
        render(allCustomers)

        etName.text.clear()
        etPhone.text.clear()
        etLocation.text.clear()

        Toast.makeText(this, "Customer $name saved - Real Time: $date", Toast.LENGTH_LONG).show()
    }

    private fun loadCustomers() {
        allCustomers.clear()
        val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)

        for ((key, value) in custPrefs.all) {
            try {
                val str = value.toString()
                if (str.contains("|")) {
                    val parts = str.split("|")
                    val name = parts.getOrNull(0) ?: key
                    val phone = parts.getOrNull(1) ?: ""
                    val loc = parts.getOrNull(2) ?: ""
                    val date = parts.getOrNull(3) ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    allCustomers.add(CustomerItem(key, name, phone, loc, date))
                }
            } catch (e: Exception) {}
        }

        // Demo if empty
        if (allCustomers.isEmpty()) {
            allCustomers.add(CustomerItem("C1", "Tendai Moyo", "0771234567", "Harare CBD", "2026-05-13"))
            allCustomers.add(CustomerItem("C2", "Grace Ncube", "0712345678", "Mbare", "2026-05-12"))
        }

        // Sort newest first
        allCustomers.sortByDescending { it.id }
    }

    private fun render(list: List<CustomerItem>) {
        container.removeAllViews()
        for (c in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(12,16,12,16)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,0,6)
                layoutParams = lp
            }
            fun cell(txt: String, w: Float, bold: Boolean = false, color: Int = Color.BLACK) {
                val tv = TextView(this@CustomersActivity).apply {
                    text = txt; textSize = 12f; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
                }
                row.addView(tv)
            }
            cell(c.name, 1.5f, true, Color.parseColor("#00796B"))
            cell(c.phone, 1.2f)
            cell(c.location, 1.5f)
            cell(c.createdDate, 1f, false, Color.GRAY)

            // Long press to delete
            row.setOnLongClickListener {
                val custPrefs = getSharedPreferences("customers_data", MODE_PRIVATE)
                custPrefs.edit().remove(c.id).apply()
                allCustomers.remove(c)
                render(allCustomers)
                Toast.makeText(this, "Deleted ${c.name}", Toast.LENGTH_SHORT).show()
                true
            }

            container.addView(row)
        }
        tvCount.text = "${list.size} customers | Long press to delete | Real Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }
}