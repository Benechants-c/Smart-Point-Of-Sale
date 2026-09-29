package com.smartpos

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AdminDetailActivity : android..app.Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("TITLE") ?: "Admin"
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
            setPadding(16,16,16,16)
        }

        // TOP BAR
        val top = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,20,20,20)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = title.uppercase()
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        top.addView(Button(this).apply {
            text = "BACK"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        })
        root.addView(top)

        // CONTENT BASED ON TITLE
        when (title) {
            "Users & Permissions", "USERS & PERMISSIONS", "Add User", "ADD USER" -> buildUsers(root)
            "Customers", "CUSTOMERS" -> buildCustomers(root)
            "Sales Management", "SALES MANAGEMENT" -> buildSales(root)
            "Purchasing", "PURCHASING" -> buildPurchasing(root)
            "Cash Management", "CASH MANAGEMENT" -> buildCash(root)
            "Branches / Shops", "BRANCHES / SHOPS", "Branches/Shops" -> buildBranches(root)
            "Reports", "REPORTS" -> buildReports(root)
            "Price Management", "PRICE MANAGEMENT", "Pricing", "PRICING" -> buildPrice(root)
            "System Settings", "SYSTEM SETTINGS" -> buildSettings(root)
            "Audit Log", "AUDIT LOG" -> buildAudit(root)
            "Products / Inventory", "PRODUCTS & STOCK" -> buildProducts(root)
            "Suppliers", "SUPPLIERS", "Add Supplier" -> buildSuppliers(root)
            "Add Product" -> buildAddProduct(root)
            "Stock Transfers", "STOCK TRANSFER" -> buildTransfer(root)
            else -> buildGeneric(root, title)
        }

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun sectionTitle(root: LinearLayout, t: String) {
        root.addView(TextView(this).apply {
            text = t
            setTypeface(null, Typeface.BOLD)
            textSize = 15f
            setPadding(0,20,0,10)
            setTextColor(Color.parseColor("#0F172A"))
        })
    }

    private fun input(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,4,0,4) }
        }
    }

    private fun btn(text: String, color: String, click: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setBackgroundColor(Color.parseColor(color))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0,18,0,18)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,6,0,6) }
            setOnClickListener { click() }
        }
    }

    private fun card(root: LinearLayout, text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
            setTextColor(Color.parseColor("#334155"))
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,4,0,4) }
        })
    }

    // 1. USERS & PERMISSIONS
    private fun buildUsers(root: LinearLayout) {
        sectionTitle(root, "Users & Permissions → cashier/admin accounts")
        val name = input("Full Name: e.g., John Cashier")
        val user = input("Username: e.g., cashier1")
        val pass = input("Password")
        val roleSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdminDetailActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("CASHIER", "ADMIN", "MANAGER"))
        }
        root.addView(name); root.addView(user); root.addView(pass); root.addView(roleSpinner)
        
        root.addView(TextView(this).apply { text = "\nPermissions:"; setTypeface(null, Typeface.BOLD) })
        val checks = listOf("Can Sell", "Can Refund", "Can View Reports", "Can Manage Stock", "Can Manage Price", "Can Cash In/Out")
        val checkBoxes = checks.map { CheckBox(this).apply { text = it; isChecked = true; setBackgroundColor(Color.WHITE) } }
        checkBoxes.forEach { root.addView(it) }

        root.addView(btn("SAVE USER", "#16A34A") {
            val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
            prefs.edit().putString(user.text.toString(), "${name.text}|${roleSpinner.selectedItem}|${pass.text}").apply()
            logAction("Created user ${user.text}")
            Toast.makeText(this, "User ${user.text} saved as ${roleSpinner.selectedItem}", Toast.LENGTH_LONG).show()
            finish()
        })
        sectionTitle(root, "Existing Users")
        val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
        prefs.all.forEach { card(root, "👤 ${it.key} - ${it.value}") }
    }

    // 2. CUSTOMERS
    private fun buildCustomers(root: LinearLayout) {
        sectionTitle(root, "Customers → records and balances")
        root.addView(input("Customer Name"))
        root.addView(input("Phone"))
        root.addView(input("Balance $"))
        root.addView(btn("ADD CUSTOMER", "#2563EB") { logAction("Added customer"); Toast.makeText(this,"Customer saved",Toast.LENGTH_SHORT).show() })
        sectionTitle(root, "Recent Customers")
        card(root, "Blessing Moyo - $12.50 balance - 0771234567")
        card(root, "Tinashe - $0.00 - Paid up")
        card(root, "Shop Account - $240.00 credit")
    }

    // 3. SALES MANAGEMENT
    private fun buildSales(root: LinearLayout) {
        sectionTitle(root, "Sales Management → sales history and receipt lookup")
        root.addView(input("Search Receipt # or Date"))
        root.addView(btn("SEARCH RECEIPT", "#7C3AED") { Toast.makeText(this,"Searching...",Toast.LENGTH_SHORT).show() })
        sectionTitle(root, "Today's Sales")
        card(root, "#004582 - $45.60 - 10:18 - Cashier: John")
        card(root, "#004581 - $120.00 - 09:55 - Cashier: Admin")
        card(root, "#004580 - $15.20 - 09:40 - Cashier: John")
        root.addView(btn("VIEW ALL SALES", "#334155") { })
    }

    // 4. PURCHASING
    private fun buildPurchasing(root: LinearLayout) {
        sectionTitle(root, "Purchasing → receiving and purchase history")
        root.addView(btn("+ NEW RECEIVING", "#2563EB") { startActivity(Intent(this, ReceiveStockActivity::class.java)) })
        sectionTitle(root, "Purchase History")
        card(root, "12/09 - Coca-Cola - +24 - $18.00 - Supplier: Delta")
        card(root, "11/09 - Bread - +50 - $25.00 - No Supplier")
        card(root, "10/09 - Mazoe - +30 - $45.00 - Supplier: Schweppes")
    }

    // 5. CASH MANAGEMENT
    private fun buildCash(root: LinearLayout) {
        sectionTitle(root, "Cash Management → cash-in/cash-out and drawer balance")
        val bal = TextView(this).apply {
            text = "DRAWER BALANCE: $1,245.60"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
            setTextColor(Color.parseColor("#16A34A"))
        }
        root.addView(bal)
        root.addView(input("Amount"))
        root.addView(input("Reason"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(Button(this).apply { text = "CASH IN"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(2,2,2,2) }; setOnClickListener { logAction("Cash IN"); Toast.makeText(this@AdminDetailActivity,"Cash IN recorded",Toast.LENGTH_SHORT).show() } })
        row.addView(Button(this).apply { text = "CASH OUT"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(2,2,2,2) }; setOnClickListener { logAction("Cash OUT"); Toast.makeText(this@AdminDetailActivity,"Cash OUT recorded",Toast.LENGTH_SHORT).show() } })
        root.addView(row)
    }

    // 6. BRANCHES
    private fun buildBranches(root: LinearLayout) {
        sectionTitle(root, "Branches / Shops → your 3-4 shops")
        root.addView(input("Shop Name"))
        root.addView(input("Location"))
        root.addView(btn("ADD SHOP", "#0F766E") { logAction("Added shop"); Toast.makeText(this,"Shop added",Toast.LENGTH_SHORT).show() })
        sectionTitle(root, "Your Shops")
        card(root, "🏪 Main Shop - Harare - Active - $18,750 stock value")
        card(root, "🏪 Shop 2 - Chitungwiza - Active - $5,200 stock value")
        card(root, "🏪 Shop 3 - Norton - Low Stock Alert - $2,100 stock value")
        root.addView(btn("+ ADD 4TH SHOP", "#334155") {})
    }

    // 7. REPORTS
    private fun buildReports(root: LinearLayout) {
        sectionTitle(root, "Reports → sales, profit, stock, purchasing and cashier reports")
        root.addView(btn("📊 SALES REPORT", "#2563EB") { Toast.makeText(this,"Sales: $2,485 today",Toast.LENGTH_LONG).show() })
        root.addView(btn("💰 PROFIT REPORT", "#16A34A") { Toast.makeText(this,"Profit: $624.30 today",Toast.LENGTH_LONG).show() })
        root.addView(btn("📦 STOCK REPORT", "#EA580C") { Toast.makeText(this,"1,248 products, 37 low stock",Toast.LENGTH_LONG).show() })
        root.addView(btn("🛒 PURCHASING REPORT", "#7C3AED") { Toast.makeText(this,"Purchasing: $88 this week",Toast.LENGTH_LONG).show() })
        root.addView(btn("👤 CASHIER REPORT", "#0F766E") { Toast.makeText(this,"John: $1,200 sales, Admin: $1,285 sales",Toast.LENGTH_LONG).show() })
    }

    // 8. PRICE MANAGEMENT
    private fun buildPrice(root: LinearLayout) {
        sectionTitle(root, "Price Management → bulk price changes")
        root.addView(input("Category: e.g., Drinks"))
        root.addView(input("Change by %: e.g., +10 or -5"))
        root.addView(btn("APPLY BULK PRICE CHANGE", "#DC2626") { logAction("Bulk price change ${root.getChildAt(3)}"); Toast.makeText(this,"Bulk price updated!",Toast.LENGTH_LONG).show() })
        sectionTitle(root, "Quick Actions")
        root.addView(btn("INCREASE ALL DRINKS +10%", "#EA580C") {})
        root.addView(btn("RESET TO COST +30%", "#334155") {})
    }

    // 9. SETTINGS
    private fun buildSettings(root: LinearLayout) {
        sectionTitle(root, "System Settings → POS configuration")
        card(root, "🏷 Shop Name: SMART POS")
        card(root, "💱 Currency: USD ($)")
        card(root, "🧾 Receipt Footer: Thank you!")
        card(root, "🖨 Printer: Not connected")
        root.addView(input("Shop Name"))
        root.addView(input("Receipt Message"))
        root.addView(btn("SAVE SETTINGS", "#1E293B") { logAction("Changed settings"); Toast.makeText(this,"Settings saved",Toast.LENGTH_SHORT).show() })
    }

    // 10. AUDIT LOG
    private fun buildAudit(root: LinearLayout) {
        sectionTitle(root, "Audit Log → who changed what and when")
        val prefs = getSharedPreferences("audit", Context.MODE_PRIVATE)
        val logs = prefs.getStringSet("logs", setOf(
            "10:25 Admin - Stock received Coca-Cola +24",
            "10:18 Cashier John - Sale #004582 $45.60",
            "09:55 Admin - Price changed Mazoe $3.00 → $3.50",
            "09:40 Admin - Created user cashier1"
        )) ?: setOf()
        logs.forEach { card(root, "📝 $it") }
        root.addView(btn("CLEAR LOG", "#DC2626") {
            prefs.edit().clear().apply()
            Toast.makeText(this,"Log cleared",Toast.LENGTH_SHORT).show()
            finish()
        })
    }

    private fun buildProducts(root: LinearLayout) {
        sectionTitle(root, "Products & Stock")
        root.addView(btn("+ ADD PRODUCT", "#16A34A") { buildAddProduct(root) })
        root.addView(btn("VIEW ALL PRODUCTS", "#2563EB") { })
        card(root, "Coca-Cola 500ml - $1.50 - 124 in stock")
        card(root, "Mazoe Orange 2L - $3.50 - 37 low")
    }

    private fun buildSuppliers(root: LinearLayout) {
        sectionTitle(root, "Suppliers")
        root.addView(input("Supplier Name"))
        root.addView(input("Phone / Contact"))
        root.addView(btn("ADD SUPPLIER", "#EA580C") { logAction("Added supplier"); Toast.makeText(this,"Supplier saved",Toast.LENGTH_SHORT).show() })
        card(root, "Delta Beverages - 0770000001")
        card(root, "Schweppes - 0770000002")
    }

    private fun buildAddProduct(root: LinearLayout) {
        sectionTitle(root, "Add Product")
        root.addView(input("Product Name"))
        root.addView(input("Barcode"))
        root.addView(input("Buying Price"))
        root.addView(input("Selling Price"))
        root.addView(input("Stock Qty"))
        root.addView(btn("SAVE PRODUCT", "#16A34A") { logAction("Added product"); Toast.makeText(this,"Product saved",Toast.LENGTH_SHORT).show(); finish() })
    }

    private fun buildTransfer(root: LinearLayout) {
        sectionTitle(root, "Stock Transfer Between Shops")
        root.addView(input("From Shop"))
        root.addView(input("To Shop"))
        root.addView(input("Product + Qty"))
        root.addView(btn("TRANSFER STOCK", "#0F766E") { logAction("Stock transfer"); Toast.makeText(this,"Transferred",Toast.LENGTH_SHORT).show() })
    }

    private fun buildGeneric(root: LinearLayout, title: String) {
        sectionTitle(root, title)
        card(root, "This screen for $title is ready.\nWe will add full functionality next.")
        root.addView(btn("BACK TO DASHBOARD", "#2563EB") { finish() })
    }

    private fun logAction(action: String) {
        val prefs = getSharedPreferences("audit", Context.MODE_PRIVATE)
        val time = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date())
        val existing = prefs.getStringSet("logs", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add("$time Admin - $action")
        prefs.edit().putStringSet("logs", existing).apply()
    }
}
