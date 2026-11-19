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

class AdminActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var drawer: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var topBar: LinearLayout

    private var drawerOpen = true
    private var currentPage = "Dashboard"

    private val blue = Color.parseColor("#2563EB")
    private val dark = Color.parseColor("#0F172A")
    private val bg = Color.parseColor("#F1F5F9")
    private val white = Color.WHITE
    private val gray = Color.parseColor("#64748B")
    private val green = Color.parseColor("#16A34A")
    private val orange = Color.parseColor("#EA580C")

    // ------------------------------------------------
    // SAMPLE DATA
    // Replace with Firebase/live database later
    // ------------------------------------------------

    private val products = mutableListOf(
        "Sugar 2kg | $3.50 | Stock: 25",
        "Rice 5kg | $7.00 | Stock: 18",
        "Cooking Oil 2L | $4.50 | Stock: 8",
        "Salt 1kg | $0.80 | Stock: 42",
        "Soap | $1.20 | Stock: 6"
    )

    private val customers = mutableListOf(
        "John Moyo | 0770000001",
        "Mary Ndlovu | 0770000002",
        "Peter Dube | 0770000003"
    )

    private val sales = mutableListOf(
        "SALE001 | 09:15 | $25.50 | CASH",
        "SALE002 | 10:05 | $18.00 | ECOCASH",
        "SALE003 | 11:20 | $42.75 | CASH",
        "SALE004 | 12:10 | $15.00 | CARD"
    )

    private val expenses = mutableListOf<String>()
    private var expenseTotal = 0.0

    private var shopName = "SHOP001-HRE"
    private var managerName = "Shop Manager"
    private var phone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildMain()
        showPage("Dashboard")
        buildDrawer()
    }

    // ------------------------------------------------
    // MAIN LAYOUT
    // ------------------------------------------------

    private fun buildMain() {

        root = FrameLayout(this)
        root.setBackgroundColor(bg)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(bg)

        topBar = LinearLayout(this)
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
            drawer.visibility =
                if (drawerOpen) View.VISIBLE else View.GONE
        }

        val heading = TextView(this)
        heading.text = "  SMARTPOS MANAGER"
        heading.textSize = 18f
        heading.setTextColor(dark)
        heading.setTypeface(null, Typeface.BOLD)

        topBar.addView(menuButton)
        topBar.addView(
            heading,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(12), dp(16), dp(20))

        val scroll = ScrollView(this)
        scroll.addView(content)

        main.addView(topBar)
        main.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(main)

        drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setBackgroundColor(white)
        drawer.elevation = dp(12).toFloat()

        val drawerParams = FrameLayout.LayoutParams(
            dp(290),
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.START
        )

        root.addView(drawer, drawerParams)

        setContentView(root)
    }

    // ------------------------------------------------
    // SIDE MENU
    // ------------------------------------------------

    private fun buildDrawer() {

        drawer.removeAllViews()

        val header = TextView(this)
        header.text =
            "SMARTPOS\nSHOP MANAGER\n\n$shopName"
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
        addMenuItem(list, "💰", "Expenses")
        addMenuItem(list, "⚙", "Basic Shop Settings")

        val logout = Button(this)
        logout.text = "↪ Logout"
        logout.textSize = 17f
        logout.setTextColor(Color.WHITE)
        logout.setBackgroundColor(Color.parseColor("#DC2626"))
        logout.setOnClickListener {
            finish()
        }

        list.addView(logout)

        scroll.addView(list)

        drawer.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
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

        button.setTextColor(
            if (currentPage == name) blue else dark
        )

        button.setBackgroundColor(
            if (currentPage == name)
                Color.parseColor("#DBEAFE")
            else white
        )

        button.setOnClickListener {
            showPage(name)
            drawerOpen = false
            drawer.visibility = View.GONE
        }

        parent.addView(button)

        val divider = View(this)
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"))

        parent.addView(
            divider,
            LinearLayout.LayoutParams(-1, dp(1))
        )
    }

    // ------------------------------------------------
    // PAGE ROUTER
    // ------------------------------------------------

    private fun showPage(page: String) {

        currentPage = page
        content.removeAllViews()

        buildDrawer()

        when (page) {

            "Dashboard" -> dashboardPage()

            "Daily Sales Report" -> dailyReportPage()

            "Customer List" -> customerPage()

            "Product List" -> productPage()

            "Sales History" -> salesPage()

            "Stock Overview" -> stockPage()

            "Expenses" -> expensesPage()

            "Basic Shop Settings" -> settingsPage()

            else -> dashboardPage()
        }
    }

    // ------------------------------------------------
    // DASHBOARD
    // ------------------------------------------------

    private fun dashboardPage() {

        title("Manager Dashboard")
        subtitle("Shop overview")

        card("Shop") {
            label(shopName, 20f, blue, true)
            label("Manager: $managerName")
        }

        card("Today's Overview") {
            label("Sales transactions: ${sales.size}")
            label("Products listed: ${products.size}")
            label("Expenses recorded: ${expenses.size}")
            label("Expenses total: ${money(expenseTotal)}")
        }

        actionButton("Daily Sales Report", blue) {
            showPage("Daily Sales Report")
        }

        actionButton("Product List", green) {
            showPage("Product List")
        }

        actionButton("Stock Overview", orange) {
            showPage("Stock Overview")
        }

        actionButton("Expenses", dark) {
            showPage("Expenses")
        }
    }

    // ------------------------------------------------
    // DAILY SALES REPORT / Z REPORT
    // ------------------------------------------------

    private fun dailyReportPage() {

        title("Daily Sales Report")
        subtitle("Daily sales summary and Z report")

        card("Today's Sales Summary") {
            label("Transactions: ${sales.size}")
            label("Sales total: ${money(sampleSalesTotal())}")
            label("Expenses: ${money(expenseTotal)}")
            label(
                "Net after expenses: ${
                    money(sampleSalesTotal() - expenseTotal)
                }"
            )
        }

        actionButton("📊 Daily Z Report", blue) {
            dailyZReport()
        }

        actionButton("🖨 Print Daily Report", green) {
            Toast.makeText(
                this,
                "Print integration can be connected next",
                Toast.LENGTH_SHORT
            ).show()
        }

        card("Report Information") {
            label("Date: Today's date")
            label("Shop: $shopName")
            label("Transactions: ${sales.size}")
            label("Sales total: ${money(sampleSalesTotal())}")
        }
    }

    private fun dailyZReport() {

        AlertDialog.Builder(this)
            .setTitle("DAILY Z REPORT")
            .setMessage(
                "SHOP: $shopName\n\n" +
                "Transactions: ${sales.size}\n" +
                "Total Sales: ${money(sampleSalesTotal())}\n" +
                "Expenses: ${money(expenseTotal)}\n" +
                "Net: ${money(sampleSalesTotal() - expenseTotal)}\n\n" +
                "This is a local summary, not a fiscal Z report."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun sampleSalesTotal(): Double {
        return 101.25
    }

    // ------------------------------------------------
    // CUSTOMER LIST
    // ------------------------------------------------

    private fun customerPage() {

        title("Customer List")
        subtitle("Customers")

        val search = EditText(this)
        search.hint = "Search customer..."
        content.addView(search)

        val results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL
        content.addView(results)

        fun render(query: String) {

            results.removeAllViews()

            customers.filter {
                it.contains(query, ignoreCase = true)
            }.forEach { customer ->

                cardIn(results, customer) {
                    label(customer)
                }
            }
        }

        render("")

        search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                render(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------------------------------------------
    // PRODUCT LIST
    // ------------------------------------------------

    private fun productPage() {

        title("Product List")
        subtitle("Products")

        val search = EditText(this)
        search.hint = "Search product..."
        content.addView(search)

        val results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL
        content.addView(results)

        fun render(query: String) {

            results.removeAllViews()

            products.filter {
                it.contains(query, ignoreCase = true)
            }.forEach { product ->

                cardIn(results, product) {
                    label(product, 16f, dark, true)
                }
            }
        }

        render("")

        search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                render(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------------------------------------------
    // SALES HISTORY
    // ------------------------------------------------

    private fun salesPage() {

        title("Sales History")
        subtitle("Recorded sales")

        card("Sales Summary") {
            label("Transactions: ${sales.size}")
            label("Total: ${money(sampleSalesTotal())}")
        }

        sales.forEach { sale ->

            card(sale) {
                label(sale)
            }
        }
    }

    // ------------------------------------------------
    // STOCK OVERVIEW
    // ------------------------------------------------

    private fun stockPage() {

        title("Stock Overview")
        subtitle("Current sample inventory")

        var lowStock = 0

        products.forEach { product ->

            val quantity = product.substringAfter("Stock: ")
                .toIntOrNull() ?: 0

            if (quantity < 10) lowStock++

            card(product) {
                label(product, 15f)
                if (quantity < 10) {
                    label("LOW STOCK", 14f, orange, true)
                }
            }
        }

        card("Stock Summary") {
            label("Products: ${products.size}")
            label("Low stock products: $lowStock")
        }
    }

    // ------------------------------------------------
    // EXPENSES
    // ------------------------------------------------

    private fun expensesPage() {

        title("Expenses")
        subtitle("Shop expenses")

        card("Expense Summary") {
            label("Expense entries: ${expenses.size}")
            label("Total expenses: ${money(expenseTotal)}")
        }

        actionButton("＋ Add Expense", blue) {
            addExpenseDialog()
        }

        expenses.forEach { expense ->
            card(expense) {
                label(expense)
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

        AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->

                val name = description.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()

                if (name.isNotEmpty() && value != null && value > 0) {

                    expenseTotal += value

                    expenses.add("$name | ${money(value)}")

                    showPage("Expenses")

                    Toast.makeText(
                        this,
                        "Expense saved",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Enter a description and valid amount",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------
    // BASIC SHOP SETTINGS
    // ------------------------------------------------

    private fun settingsPage() {

        title("Basic Shop Settings")
        subtitle("Shop manager settings")

        val nameInput = EditText(this)
        nameInput.setText(shopName)
        nameInput.hint = "Shop Name"

        val managerInput = EditText(this)
        managerInput.setText(managerName)
        managerInput.hint = "Manager Name"

        val phoneInput = EditText(this)
        phoneInput.setText(phone)
        phoneInput.hint = "Shop Phone Number"

        card("Shop Details") {
            content.addView(nameInput)
            content.addView(managerInput)
            content.addView(phoneInput)
        }

        actionButton("Save Shop Settings", blue) {

            shopName = nameInput.text.toString().trim()
            managerName = managerInput.text.toString().trim()
            phone = phoneInput.text.toString().trim()

            buildDrawer()

            Toast.makeText(
                this,
                "Settings saved for this session",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ------------------------------------------------
    // UI HELPERS
    // ------------------------------------------------

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

        if (bold) {
            view.setTypeface(null, Typeface.BOLD)
        }

        view.setPadding(0, dp(5), 0, dp(5))
        content.addView(view)
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
        box.body()

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

        val title = TextView(this)
        title.text = heading
        title.textSize = 16f
        title.setTextColor(dark)
        title.setTypeface(null, Typeface.BOLD)

        box.addView(title)
        box.body()

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(8))

        parent.addView(box, params)
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

        button.setOnClickListener {
            action()
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(12))

        content.addView(button, params)
    }

    private fun money(value: Double): String {
        return "$" + String.format("%.2f", value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}