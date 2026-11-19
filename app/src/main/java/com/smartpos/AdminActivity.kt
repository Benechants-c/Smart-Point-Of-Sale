package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class AdminActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var drawer: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var activeContainer: LinearLayout

    private var drawerOpen = false
    private var currentPage = "Dashboard"

    private val blue = Color.parseColor("#2563EB")
    private val dark = Color.parseColor("#0F172A")
    private val bg = Color.parseColor("#F1F5F9")
    private val white = Color.WHITE
    private val gray = Color.parseColor("#64748B")
    private val green = Color.parseColor("#16A34A")
    private val orange = Color.parseColor("#EA580C")
    private val red = Color.parseColor("#DC2626")

    data class Product(
        var name: String,
        var code: String,
        var price: Double,
        var stock: Int
    )

    data class Expense(
        var description: String,
        var amount: Double,
        var timestamp: Long
    )

    data class Sale(
        var receipt: String,
        var time: String,
        var total: Double,
        var payment: String
    )

    private val products = mutableListOf<Product>()
    private val customers = mutableListOf<String>()
    private val sales = mutableListOf<Sale>()
    private val expenses = mutableListOf<Expense>()

    private var shopName = "SHOP001-HRE"
    private var managerName = "Shop Manager"
    private var phone = ""

    private val prefs by lazy {
        getSharedPreferences("manager_data", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadData()
        buildMain()
        showPage("Dashboard")
    }

    // ================= DATA =================

    private fun loadData() {

        shopName = prefs.getString("shopName", "SHOP001-HRE") ?: "SHOP001-HRE"
        managerName = prefs.getString("managerName", "Shop Manager") ?: "Shop Manager"
        phone = prefs.getString("phone", "") ?: ""

        val productJson = JSONArray(prefs.getString("products", "[]"))

        for (i in 0 until productJson.length()) {
            val item = productJson.getJSONObject(i)

            products.add(
                Product(
                    item.optString("name"),
                    item.optString("code"),
                    item.optDouble("price"),
                    item.optInt("stock")
                )
            )
        }

        val expenseJson = JSONArray(prefs.getString("expenses", "[]"))

        for (i in 0 until expenseJson.length()) {
            val item = expenseJson.getJSONObject(i)

            expenses.add(
                Expense(
                    item.optString("description"),
                    item.optDouble("amount"),
                    item.optLong("timestamp")
                )
            )
        }
    }

    private fun saveProducts() {
        val array = JSONArray()

        products.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("code", it.code)
            obj.put("price", it.price)
            obj.put("stock", it.stock)
            array.put(obj)
        }

        prefs.edit().putString("products", array.toString()).apply()
    }

    private fun saveExpenses() {
        val array = JSONArray()

        expenses.forEach {
            val obj = JSONObject()
            obj.put("description", it.description)
            obj.put("amount", it.amount)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }

        prefs.edit().putString("expenses", array.toString()).apply()
    }

    // ================= MAIN LAYOUT =================

    private fun buildMain() {

        root = FrameLayout(this)
        root.setBackgroundColor(bg)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(bg)

        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(12), dp(8), dp(12), dp(8))
        topBar.setBackgroundColor(white)

        val menuButton = Button(this)
        menuButton.text = "☰"
        menuButton.textSize = 20f
        menuButton.setTextColor(white)
        menuButton.setBackgroundColor(blue)

        menuButton.setOnClickListener {
            drawerOpen = !drawerOpen
            drawer.visibility = if (drawerOpen) View.VISIBLE else View.GONE
        }

        val heading = TextView(this)
        heading.text = "  SMARTPOS MANAGER"
        heading.textSize = 18f
        heading.setTextColor(dark
        )
        heading.setTypeface(null, Typeface.BOLD)

        topBar.addView(menuButton)
        topBar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(12), dp(16), dp(20))

        // Important: initialize the card content target before any card is built.
        activeContainer = content

        val scroll = ScrollView(this)
        scroll.addView(content)

        main.addView(topBar)
        main.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(main)

        drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setBackgroundColor(white)
        drawer.elevation = dp(12).toFloat()
        drawer.visibility = View.GONE

        val drawerParams = FrameLayout.LayoutParams(
            dp(290),
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.START
        )

        root.addView(drawer, drawerParams)

        setContentView(root)
    }

    // ================= DRAWER =================

    private fun buildDrawer() {

        drawer.removeAllViews()

        val header = TextView(this)
        header.text = "SMARTPOS\nSHOP MANAGER\n\n$shopName"
        header.textSize = 21f
        header.setTypeface(null, Typeface.BOLD)
        header.setTextColor(white)
        header.setPadding(dp(20), dp(25), dp(15), dp(25))
        header.setBackgroundColor(blue)
        drawer.addView(header)

        val scroll = ScrollView(this)
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL

        addMenuItem(list, "🏠", "Dashboard")
        addMenuItem(list, "📊", "Daily Sales Report")
        addMenuItem(list, "👥", "Customer List")
        addMenuItem(list, "📦", "Product List")
        addMenuItem(list, "🧾", "Sales History")
        addMenuItem(list, "📚", "Stock Overview")
        addMenuItem(list, "🛠", "Stock & Price Management")
        addMenuItem(list, "💰", "Expenses")
        addMenuItem(list, "⚙", "Basic Shop Settings")

        val logout = Button(this)
        logout.text = "↪ Logout"
        logout.textSize = 17f
        logout.setTextColor(white)
        logout.setBackgroundColor(red)
        logout.setOnClickListener { finish() }

        list.addView(logout)
        scroll.addView(list)

        drawer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun addMenuItem(
        parent: LinearLayout,
        icon: String,
        name: String
    ) {
        val button = Button(this)
        button.text = "$icon   $name"
        button.textSize = 15f
        button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        button.setPadding(dp(16), dp(10), dp(8), dp(10))

        button.setTextColor(if (currentPage == name) blue else dark)
        button.setBackgroundColor(
            if (currentPage == name) Color.parseColor("#DBEAFE") else white
        )

        button.setOnClickListener {
            showPage(name)
            drawerOpen = false
            drawer.visibility = View.GONE
        }

        parent.addView(button)

        val divider = View(this)
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"))
        parent.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
    }

    // ================= PAGE ROUTER =================

    private fun showPage(page: String) {

        currentPage = page
        content.removeAllViews()
        activeContainer = content
        buildDrawer()

        when (page) {
            "Dashboard" -> dashboardPage()
            "Daily Sales Report" -> dailyReportPage()
            "Customer List" -> customerPage()
            "Product List" -> productPage()
            "Sales History" -> salesPage()
            "Stock Overview" -> stockPage()
            "Stock & Price Management" -> stockManagementPage()
            "Expenses" -> expensesPage()
            "Basic Shop Settings" -> settingsPage()
            else -> dashboardPage()
        }
    }

    // ================= DASHBOARD =================

    private fun dashboardPage() {

        title("Manager Dashboard")
        subtitle("Shop overview")

        card("Shop Details") {
            label(shopName, 20f, blue, true)
            label("Manager: $managerName")
            if (phone.isNotEmpty()) label("Phone: $phone")
        }

        card("Today's Overview") {
            label("Sales transactions: ${sales.size}")
            label("Products listed: ${products.size}")
            label("Customers: ${customers.size}")
            label("Today's expenses: ${money(expenseForToday())}")
        }

        card("Stock Alerts") {
            val low = products.filter { it.stock < 10 }

            label("Low stock products: ${low.size}", 16f, orange, true)

            if (low.isEmpty()) {
                label("No low stock products")
            } else {
                low.forEach {
                    label("${it.name} - ${it.stock} remaining", 14f, orange)
                }
            }
        }

        card("Expense Overview") {
            label("Today: ${money(expenseForToday())}")
            label("This week: ${money(expenseForWeek())}")
            label("This month: ${money(expenseForMonth())}")
            label("All time: ${money(expenseAllTime())}")
        }
    }

    // ================= DAILY SALES REPORT =================

    private fun dailyReportPage() {

        title("Daily Sales Report")
        subtitle("Sales summary")

        card("Today's Sales Summary") {
            label("Transactions: ${sales.size}")
            label("Sales total: ${money(salesTotal())}")
            label("Today's expenses: ${money(expenseForToday())}")
            label("Net after expenses: ${money(salesTotal() - expenseForToday())}")
        }

        actionButton("📊 Daily Z Report", blue) {
            dailyZReport()
        }

        actionButton("🖨 Print Daily Report", green) {
            Toast.makeText(this, "Print integration can be connected to your POS", Toast.LENGTH_SHORT).show()
        }

        card("Report Information") {
            label("Date: ${todayDate()}")
            label("Shop: $shopName")
            label("Transactions: ${sales.size}")
            label("Sales total: ${money(salesTotal())}")
        }
    }

    private fun dailyZReport() {
        AlertDialog.Builder(this)
            .setTitle("DAILY Z REPORT")
            .setMessage(
                "SHOP: $shopName\n\n" +
                "Date: ${todayDate()}\n" +
                "Transactions: ${sales.size}\n" +
                "Total Sales: ${money(salesTotal())}\n" +
                "Expenses: ${money(expenseForToday())}\n" +
                "Net: ${money(salesTotal() - expenseForToday())}\n\n" +
                "This is a manager summary, not a fiscal Z report."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun salesTotal(): Double = sales.sumOf { it.total }

    // ================= CUSTOMERS =================

    private fun customerPage() {

        title("Customer List")
        subtitle("Search registered customers")

        val search = EditText(this)
        search.hint = "Search customer..."
        content.addView(search)

        val results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL
        content.addView(results)

        fun render(query: String) {
            results.removeAllViews()

            val filtered = customers.filter {
                it.contains(query, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                emptyMessage(results, "No customers found")
            } else {
                filtered.forEach {
                    cardIn(results, it) {
                        textRow(it)
                    }
                }
            }
        }

        render("")

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ================= PRODUCTS =================

    private fun productPage() {

        title("Product List")
        subtitle("Shop products")

        val search = EditText(this)
        search.hint = "Search product name or code..."
        content.addView(search)

        val results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL
        content.addView(results)

        fun render(query: String) {
            results.removeAllViews()

            val filtered = products.filter {
                it.name.contains(query, true) || it.code.contains(query, true)
            }

            if (filtered.isEmpty()) {
                emptyMessage(results, "No products found")
            } else {
                filtered.forEach { product ->
                    cardIn(results, product.name) {
                        productRow(product)
                    }
                }
            }
        }

        render("")

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ================= SALES HISTORY =================

    private fun salesPage() {

        title("Sales History")
        subtitle("Recorded transactions")

        card("Sales Summary") {
            label("Transactions: ${sales.size}")
            label("Total sales: ${money(salesTotal())}")
        }

        if (sales.isEmpty()) {
            emptyMessage(content, "No sales records available")
        } else {
            sales.forEach { sale ->
                cardIn(content, sale.receipt) {
                    textRow("Time: ${sale.time}")
                    textRow("Amount: ${money(sale.total)}", blue, true)
                    textRow("Payment: ${sale.payment}")
                }
            }
        }
    }

    // ================= STOCK OVERVIEW =================

    private fun stockPage() {

        title("Stock Overview")
        subtitle("Current inventory")

        var lowStock = 0
        var totalUnits = 0

        if (products.isEmpty()) {
            emptyMessage(content, "No stock records available")
        } else {
            products.forEach { product ->
                totalUnits += product.stock
                if (product.stock < 10) lowStock++

                cardIn(content, product.name) {
                    productRow(product)
                    if (product.stock < 10) {
                        textRow("LOW STOCK", orange, true)
                    }
                }
            }
        }

        card("Stock Summary") {
            label("Products: ${products.size}")
            label("Total units: $totalUnits")
            label("Low stock products: $lowStock", 16f, orange, true)
        }
    }

    // ================= STOCK & PRICE MANAGEMENT =================

    private fun stockManagementPage() {

        title("Stock & Price Management")
        subtitle("Edit product quantities and selling prices")

        actionButton("＋ Add New Product", blue) {
            editProductDialog(null)
        }

        val search = EditText(this)
        search.hint = "Search product..."
        content.addView(search)

        val results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL
        content.addView(results)

        fun render(query: String) {
            results.removeAllViews()

            val filtered = products.filter {
                it.name.contains(query, true) || it.code.contains(query, true)
            }

            if (filtered.isEmpty()) {
                emptyMessage(results, "No products found")
            } else {
                filtered.forEach { product ->
                    cardIn(results, product.name) {
                        textRow("Code: ${product.code}")
                        textRow("Selling price: ${money(product.price)}", blue, true)
                        textRow("Available stock: ${product.stock}")

                        val edit = Button(this@AdminActivity)
                        edit.text = "Edit Stock / Price"
                        edit.setTextColor(white)
                        edit.setBackgroundColor(blue)
                        edit.setOnClickListener { editProductDialog(product) }
                        addView(edit)
                    }
                }
            }
        }

        render("")

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun editProductDialog(product: Product?) {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(dp(20), dp(10), dp(20), dp(10))

        val name = EditText(this)
        name.hint = "Product name"
        name.setText(product?.name ?: "")

        val code = EditText(this)
        code.hint = "Product code"
        code.setText(product?.code ?: "")

        val price = EditText(this)
        price.hint = "Selling price"
        price.inputType = 8194
        price.setText(if (product != null) product.price.toString() else "")

        val stock = EditText(this)
        stock.hint = "Stock quantity"
        stock.inputType = 2
        stock.setText(if (product != null) product.stock.toString() else "")

        layout.addView(name)
        layout.addView(code)
        layout.addView(price)
        layout.addView(stock)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (product == null) "Add Product" else "Edit Product")
            .setView(layout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val productName = name.text.toString().trim()
                val productCode = code.text.toString().trim()
                val productPrice = price.text.toString().toDoubleOrNull()
                val productStock = stock.text.toString().toIntOrNull()

                if (
                    productName.isEmpty() ||
                    productPrice == null || productPrice < 0 ||
                    productStock == null || productStock < 0
                ) {
                    Toast.makeText(
                        this,
                        "Enter a valid name, price and stock",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                if (product == null) {
                    products.add(Product(productName, productCode, productPrice, productStock))
                } else {
                    product.name = productName
                    product.code = productCode
                    product.price = productPrice
                    product.stock = productStock
                }

                saveProducts()
                dialog.dismiss()
                showPage("Stock & Price Management")
            }
        }

        dialog.show()
    }

    // ================= EXPENSES =================

    private fun expensesPage() {

        title("Expenses")
        subtitle("Expense tracking and summaries")

        card("Expense Summary") {
            label("Today", 14f, gray)
            label(money(expenseForToday()), 22f, blue, true)

            dividerLine()

            label("This Week", 14f, gray)
            label(money(expenseForWeek()), 20f, dark, true)

            dividerLine()

            label("This Month", 14f, gray)
            label(money(expenseForMonth()), 20f, dark, true)

            dividerLine()

            label("All-Time Expenses", 14f, gray)
            label(money(expenseAllTime()), 22f, green, true)
        }

        actionButton("＋ Add Expense", blue) {
            addExpenseDialog()
        }

        card("Expense Records") {
            if (expenses.isEmpty()) {
                label("No expenses recorded yet", 14f, gray)
            } else {
                expenses.sortedByDescending { it.timestamp }.forEach {
                    label(it.description, 16f, dark, true)
                    label("${formatDate(it.timestamp)}   ${money(it.amount)}", 14f, gray)
                    dividerLine()
                }
            }
        }
    }

    private fun addExpenseDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(dp(20), dp(10), dp(20), dp(10))

        val description = EditText(this)
        description.hint = "Expense description"

        val amount = EditText(this)
        amount.hint = "Amount"
        amount.inputType = 8194

        layout.addView(description)
        layout.addView(amount)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(layout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val name = description.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()

                if (name.isEmpty() || value == null || value <= 0) {
                    Toast.makeText(
                        this,
                        "Enter a description and valid amount",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                expenses.add(Expense(name, value, System.currentTimeMillis()))
                saveExpenses()
                dialog.dismiss()
                showPage("Expenses")
            }
        }

        dialog.show()
    }

    private fun expenseForToday(): Double {
        return expenses.filter {
            isSameDay(it.timestamp, System.currentTimeMillis())
        }.sumOf { it.amount }
    }

    private fun expenseForWeek(): Double {

        val now = Calendar.getInstance()

        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.DAY_OF_MONTH, -6)
            }
        }

        return expenses.filter {
            it.timestamp >= start.timeInMillis &&
                    it.timestamp <= now.timeInMillis
        }.sumOf { it.amount }
    }

    private fun expenseForMonth(): Double {

        val now = Calendar.getInstance()

        return expenses.filter {
            val date = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            date.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }.sumOf { it.amount }
    }

    private fun expenseAllTime(): Double = expenses.sumOf { it.amount }

    // ================= SETTINGS =================

    private fun settingsPage() {

        title("Basic Shop Settings")
        subtitle("Manager-accessible shop information")

        val nameInput = EditText(this)
        nameInput.hint = "Shop Name"
        nameInput.setText(shopName)

        val managerInput = EditText(this)
        managerInput.hint = "Manager Name"
        managerInput.setText(managerName)

        val phoneInput = EditText(this)
        phoneInput.hint = "Shop Phone"
        phoneInput.setText(phone)

        card("Shop Details") {
            addView(nameInput)
            addView(managerInput)
            addView(phoneInput)
        }

        actionButton("Save Shop Settings", blue) {

            shopName = nameInput.text.toString().trim()
            managerName = managerInput.text.toString().trim()
            phone = phoneInput.text.toString().trim()

            prefs.edit()
                .putString("shopName", shopName)
                .putString("managerName", managerName)
                .putString("phone", phone)
                .apply()

            buildDrawer()

            Toast.makeText(this, "Shop settings saved", Toast.LENGTH_SHORT).show()
        }
    }

    // ================= UI HELPERS =================

    private fun title(text: String) {
        val view = TextView(this)
        view.text = text
        view.textSize = 23f
        view.setTextColor(dark)
        view.setTypeface(null, Typeface.BOLD)
        view.setPadding(0, dp(15), 0, dp(8))
        content.addView(view)
    }

    private fun subtitle(text: String) {
        val view = TextView(this)
        view.text = text
        view.textSize = 15f
        view.setTextColor(gray)
        view.setPadding(0, 0, 0, dp(15))
        content.addView(view)
    }

    private fun label(
        text: String,
        size: Float = 16f,
        color: Int = dark,
        bold: Boolean = false
    ) {
        val view = TextView(this)
        view.text = text
        view.textSize = size
        view.setTextColor(color)
        if (bold) view.setTypeface(null, Typeface.BOLD)
        view.setPadding(0, dp(5), 0, dp(5))
        activeContainer.addView(view)
    }

    private fun card(
        heading: String,
        body: LinearLayout.() -> Unit
    ) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(15), dp(12), dp(15), dp(12))
        box.setBackgroundColor(white)

        val headingView = TextView(this)
        headingView.text = heading
        headingView.textSize = 17f
        headingView.setTextColor(blue)
        headingView.setTypeface(null, Typeface.BOLD)
        box.addView(headingView)

        val previousContainer = activeContainer
        activeContainer = box
        box.body()
        activeContainer = previousContainer

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(12))
        content.addView(box, params)
    }

    private fun cardIn(
        parent: LinearLayout,
        heading: String,
        body: LinearLayout.() -> Unit
    ) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(15), dp(12), dp(15), dp(12))
        box.setBackgroundColor(white)

        val headingView = TextView(this)
        headingView.text = heading
        headingView.textSize = 16f
        headingView.setTextColor(dark)
        headingView.setTypeface(null, Typeface.BOLD)
        box.addView(headingView)

        val previousContainer = activeContainer
        activeContainer = box
        box.body()
        activeContainer = previousContainer

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(8))
        parent.addView(box, params)
    }

    private fun textRow(
        text: String,
        color: Int = dark,
        bold: Boolean = false
    ) {
        val view = TextView(this)
        view.text = text
        view.textSize = 15f
        view.setTextColor(color)
        view.setPadding(0, dp(5), 0, dp(5))
        if (bold) view.setTypeface(null, Typeface.BOLD)
        activeContainer.addView(view)
    }

    private fun productRow(product: Product) {
        textRow("Code: ${product.code.ifBlank { "N/A" }}", gray)
        textRow("Selling price: ${money(product.price)}", blue, true)
        textRow("Stock quantity: ${product.stock}", dark, true)
    }

    private fun emptyMessage(parent: LinearLayout, message: String) {
        val view = TextView(this)
        view.text = message
        view.textSize = 15f
        view.setTextColor(gray)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(12), dp(25), dp(12), dp(25))
        parent.addView(view)
    }

    private fun dividerLine() {
        val line = View(this)
        line.setBackgroundColor(Color.parseColor("#E2E8F0"))
        activeContainer.addView(line, LinearLayout.LayoutParams(-1, dp(1)))
    }

    private fun actionButton(
        text: String,
        color: Int,
        action: () -> Unit
    ) {
        val button = Button(this)
        button.text = text
        button.textSize = 16f
        button.setTextColor(white)
        button.setBackgroundColor(color)
        button.setOnClickListener { action() }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(12))
        content.addView(button, params)
    }

    // ================= DATE / MONEY =================

    private fun money(value: Double): String {
        return "$" + String.format(Locale.US, "%.2f", value)
    }

    private fun todayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(System.currentTimeMillis())
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(timestamp)
    }

    private fun isSameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }

        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}