package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.text.TextWatcher
import android.text.Editable
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class SalesActivity : Activity() {

    private val blue = Color.rgb(25, 118, 210)
    private val navy = Color.rgb(20, 40, 65)
    private val green = Color.rgb(46, 125, 50)
    private val red = Color.rgb(198, 40, 40)
    private val white = Color.WHITE
    private val background = Color.rgb(245, 247, 250)
    private val gray = Color.rgb(100, 100, 100)

    private lateinit var tvCashier: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvTendered: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvChange: TextView

    private lateinit var searchBox: EditText
    private lateinit var productContainer: LinearLayout
    private lateinit var cartContainer: LinearLayout
    private lateinit var receiptText: TextView

    private var cartTotal = 0.0
    private var tendered = 0.0
    private var lastCompletedReceipt = ""
    private var lastReceiptNo = ""
    private var lastTapTime = 0L
    private var paymentType = "CASH"

    private val cartItems = mutableListOf<CartItem>()
    private val products = mutableListOf<Product>()

    private val handler = Handler(Looper.getMainLooper())

    data class Product(
        val code: String,
        val name: String,
        val price: Double,
        val stock: Int = -1
    )

    data class CartItem(
        val product: Product,
        var quantity: Int = 1
    ) {
        val total: Double
            get() = product.price * quantity
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            tvDateTime.text = SimpleDateFormat(
                "EEE, dd MMM yyyy   HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = navy
        window.navigationBarColor = navy

        loadGoodsReceivedProducts()
        buildScreen()
        refreshCart()
        updateReceipt()
        updatePayment()
        handler.post(clockRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockRunnable)
        super.onDestroy()
    }

    // =========================================================
    // SCREEN
    // =========================================================

    private fun buildScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(navy)
        }

        val title = makeText(
            "PROMAX POS",
            23,
            white,
            true
        )

        val shop = makeText(
            "SALES TERMINAL",
            12,
            Color.LTGRAY,
            false
        )

        tvCashier = makeText(
            "Cashier: ${cashierName()}",
            15,
            white,
            true
        )

        tvDateTime = makeText(
            "",
            13,
            Color.LTGRAY,
            false
        )

        header.addView(title)
        header.addView(shop)
        header.addView(tvCashier)
        header.addView(tvDateTime)

        root.addView(header)

        val bodyScroll = ScrollView(this)

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 6, 8, 10)
        }

        bodyScroll.addView(body)

        // PRODUCT SEARCH
        val searchPanel = panel()

        searchPanel.addView(
            makeText("SEARCH PRODUCTS", 17, navy, true)
        )

        searchBox = EditText(this).apply {
            hint = "Type product name or code..."
            textSize = 17f
            singleLine = true
            setPadding(14, 12, 14, 12)
            setBackgroundColor(white)
        }

        searchPanel.addView(searchBox)

        productContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        searchPanel.addView(productContainer)

        body.addView(searchPanel)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                displayProducts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // CART
        val cartPanel = panel()

        cartPanel.addView(
            makeText("SHOPPING CART", 18, navy, true)
        )

        cartContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        cartPanel.addView(cartContainer)
        body.addView(cartPanel)

        // RECEIPT
        val receiptPanel = panel()

        receiptPanel.addView(
            makeText("VIRTUAL RECEIPT", 18, navy, true)
        )

        receiptText = makeText(
            "",
            13,
            Color.BLACK,
            false
        ).apply {
            typeface = Typeface.MONOSPACE
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(252, 252, 252))
        }

        receiptText.setOnClickListener {
            val now = System.currentTimeMillis()

            if (now - lastTapTime < 450) {
                reprintLastReceipt()
            } else {
                previewReceipt()
            }

            lastTapTime = now
        }

        receiptPanel.addView(receiptText)

        receiptPanel.addView(
            makeText(
                "Tap to preview • Double-tap to reprint last sale",
                11,
                gray,
                false
            )
        )

        body.addView(receiptPanel)

        // PAYMENT
        val paymentPanel = panel()

        paymentPanel.addView(
            makeText("PAYMENT", 18, navy, true)
        )

        tvTotal = makeText(
            "TOTAL: $0.00",
            26,
            blue,
            true
        )

        paymentPanel.addView(tvTotal)

        val spinner = Spinner(this)

        val paymentMethods = arrayOf(
            "CASH",
            "ECOCASH",
            "CARD",
            "OTHER"
        )

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            paymentMethods
        )

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    paymentType = paymentMethods[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        paymentPanel.addView(spinner)

        tvTendered = makeText(
            "TENDERED: $0.00",
            17,
            navy,
            true
        )

        tvBalance = makeText(
            "BALANCE DUE: $0.00",
            17,
            red,
            true
        )

        tvChange = makeText(
            "CHANGE: $0.00",
            23,
            green,
            true
        )

        paymentPanel.addView(tvTendered)
        paymentPanel.addView(tvBalance)
        paymentPanel.addView(tvChange)

        val tenderBox = EditText(this).apply {
            hint = "Enter amount received"
            textSize = 20f
            singleLine = true
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(white)
        }

        paymentPanel.addView(tenderBox)

        tenderBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                tendered = s?.toString()?.toDoubleOrNull() ?: 0.0
                updatePayment()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val completeButton = makeButton(
            "COMPLETE SALE • PRINT RECEIPT",
            green
        )

        completeButton.setOnClickListener {
            completeSale()
        }

        paymentPanel.addView(completeButton)

        val clearButton = makeButton(
            "CANCEL / CLEAR CART",
            red
        )

        clearButton.setOnClickListener {
            clearCartConfirm(tenderBox)
        }

        paymentPanel.addView(clearButton)

        body.addView(paymentPanel)

        root.addView(
            bodyScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    private fun loadGoodsReceivedProducts() {

        products.clear()

        val prefs = getSharedPreferences(
            "goods_received",
            MODE_PRIVATE
        )

        val seen = mutableSetOf<String>()

        for ((key, value) in prefs.all) {

            try {
                val raw = value?.toString() ?: continue

                if (raw.contains("|")) {

                    val parts = raw.split("|")

                    val code = parts.getOrNull(0)?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: key

                    val name = parts.getOrNull(1)?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: key

                    val price =
                        parts.getOrNull(4)?.toDoubleOrNull()
                        ?: parts.getOrNull(3)?.toDoubleOrNull()
                        ?: parts.getOrNull(2)?.toDoubleOrNull()
                        ?: continue

                    val unique = "$code|$name"

                    if (seen.add(unique)) {
                        products.add(
                            Product(code, name, price)
                        )
                    }

                } else {

                    val price = raw.toDoubleOrNull()

                    if (price != null && price > 0) {

                        val name = key
                        val unique = "$key|$name"

                        if (seen.add(unique)) {
                            products.add(
                                Product(key, name, price)
                            )
                        }
                    }
                }

            } catch (_: Exception) {
            }
        }

        // Demo products only when Goods Received has no readable products.
        if (products.isEmpty()) {
            products.addAll(
                listOf(
                    Product("1001", "Coca Cola 500ml", 1.50),
                    Product("1002", "Bread Loaf", 1.20),
                    Product("1003", "Milk 1L", 1.00),
                    Product("1004", "Sugar 2kg", 2.50),
                    Product("1005", "Rice 2kg", 3.50)
                )
            )
        }

        products.sortBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun displayProducts(query: String) {

        productContainer.removeAllViews()

        if (query.isBlank()) return

        val matches = products.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
        }.take(30)

        if (matches.isEmpty()) {
            productContainer.addView(
                makeText("PRODUCT NOT FOUND", 14, red, true)
            )
            return
        }

        matches.forEach { product ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(10, 8, 10, 8)
                setBackgroundColor(white)
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            info.addView(
                makeText(product.name, 15, navy, true)
            )

            info.addView(
                makeText(
                    "Code: ${product.code}  •  ${money(product.price)}",
                    12,
                    gray,
                    false
                )
            )

            info.layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

            val addButton = makeButton("ADD", blue)

            addButton.setOnClickListener {
                addToCart(product)
            }

            row.addView(info)
            row.addView(addButton)

            productContainer.addView(row)
            productContainer.addView(separator())
        }
    }

    private fun addToCart(product: Product) {

        val existing = cartItems.find {
            it.product.code == product.code
        }

        if (existing == null) {
            cartItems.add(CartItem(product, 1))
        } else {
            existing.quantity++
        }

        refreshCart()
        updateReceipt()
        updatePayment()
    }

    // =========================================================
    // CART
    // =========================================================

    private fun refreshCart() {

        cartContainer.removeAllViews()

        if (cartItems.isEmpty()) {

            cartContainer.addView(
                makeText("Cart is empty", 14, gray, false)
            )

            cartTotal = 0.0
            updateReceipt()
            updatePayment()
            return
        }

        cartItems.forEach { item ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
                setBackgroundColor(white)
            }

            row.addView(
                makeText(item.product.name, 15, navy, true)
            )

            row.addView(
                makeText(
                    "${money(item.product.price)} × ${item.quantity} = ${money(item.total)}",
                    14,
                    Color.DKGRAY,
                    false
                )
            )

            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val minus = makeButton("−", navy)
            val qty = makeText(
                "  ${item.quantity}  ",
                17,
                navy,
                true
            )
            val plus = makeButton("+", green)
            val remove = makeButton("REMOVE", red)

            minus.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                } else {
                    cartItems.remove(item)
                }
                refreshCart()
            }

            plus.setOnClickListener {
                item.quantity++
                refreshCart()
            }

            remove.setOnClickListener {
                cartItems.remove(item)
                refreshCart()
            }

            controls.addView(minus)
            controls.addView(qty)
            controls.addView(plus)
            controls.addView(remove)

            row.addView(controls)
            cartContainer.addView(row)
            cartContainer.addView(separator())
        }

        cartTotal = cartItems.sumOf { it.total }

        updateReceipt()
        updatePayment()
    }

    // =========================================================
    // RECEIPT
    // =========================================================

    private fun updateReceipt() {

        val receiptNo = newReceiptNumber()

        val date = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val r = StringBuilder()

        r.append("       PROMAX SUPERMARKET\n")
        r.append("            CASH SALE\n")
        r.append("--------------------------------\n")
        r.append("Receipt: $receiptNo\n")
        r.append("Cashier: ${cashierName()}\n")
        r.append("Date: $date\n")
        r.append("--------------------------------\n")

        cartItems.forEach {

            r.append("${it.product.name}\n")
            r.append("${it.quantity} x ${money(it.product.price)} = ${money(it.total)}\n")
        }

        r.append("--------------------------------\n")
        r.append("TOTAL: ${money(cartTotal)}\n")
        r.append("--------------------------------\n")
        r.append("     THANK YOU FOR SHOPPING!\n")
        r.append("           COME AGAIN\n")

        receiptText.text = r.toString()
    }

    private fun previewReceipt() {

        AlertDialog.Builder(this)
            .setTitle("Receipt Preview")
            .setMessage(receiptText.text.toString())
            .setPositiveButton("PRINT") { _, _ ->
                printReceipt(receiptText.text.toString(), false)
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    // =========================================================
    // PAYMENT
    // =========================================================

    private fun updatePayment() {

        cartTotal = cartItems.sumOf { it.total }

        val balance = (cartTotal - tendered).coerceAtLeast(0.0)
        val change = (tendered - cartTotal).coerceAtLeast(0.0)

        tvTotal.text = "TOTAL: ${money(cartTotal)}"
        tvTendered.text = "TENDERED: ${money(tendered)}"
        tvBalance.text = "BALANCE DUE: ${money(balance)}"
        tvChange.text = "CHANGE: ${money(change)}"
    }

    // =========================================================
    // COMPLETE SALE
    // =========================================================

    private fun completeSale() {

        if (cartItems.isEmpty()) {
            toast("Cart is empty")
            return
        }

        if (tendered < cartTotal) {
            toast("Amount tendered is insufficient")
            return
        }

        val receipt = receiptText.text.toString()
        val total = cartTotal
        val change = tendered - total

        AlertDialog.Builder(this)
            .setTitle("Confirm Sale")
            .setMessage(
                "Total: ${money(total)}\n" +
                        "Tendered: ${money(tendered)}\n" +
                        "Change: ${money(change)}\n" +
                        "Payment: $paymentType"
            )
            .setPositiveButton("CONFIRM SALE") { _, _ ->

                saveSale(receipt, total, tendered, change)

                lastCompletedReceipt = receipt

                printReceipt(lastCompletedReceipt, false)

                cartItems.clear()
                tendered = 0.0

                refreshCart()
                updatePayment()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun saveSale(
        receipt: String,
        total: Double,
        amountTendered: Double,
        change: Double
    ) {

        val prefs = getSharedPreferences(
            "pos_prefs",
            MODE_PRIVATE
        )

        val sales = try {
            JSONArray(prefs.getString("sales_db", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        val sale = JSONObject()

        lastReceiptNo = newReceiptNumber()

        sale.put("receiptNo", lastReceiptNo)
        sale.put("cashier", cashierName())
        sale.put("timestamp", System.currentTimeMillis())
        sale.put("paymentType", paymentType)
        sale.put("total", total)
        sale.put("tendered", amountTendered)
        sale.put("change", change)
        sale.put("receipt", receipt)

        val items = JSONArray()

        cartItems.forEach { item ->
            val obj = JSONObject()
            obj.put("code", item.product.code)
            obj.put("name", item.product.name)
            obj.put("price", item.product.price)
            obj.put("quantity", item.quantity)
            obj.put("lineTotal", item.total)
            items.put(obj)
        }

        sale.put("items", items)
        sales.put(sale)

        prefs.edit()
            .putString("sales_db", sales.toString())
            .apply()
    }

    // =========================================================
    // REPRINT
    // =========================================================

    private fun reprintLastReceipt() {

        if (lastCompletedReceipt.isBlank()) {
            toast("No completed receipt to reprint")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("REPRINT LAST RECEIPT")
            .setMessage(lastCompletedReceipt)
            .setPositiveButton("REPRINT") { _, _ ->
                printReceipt(lastCompletedReceipt, true)
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    // =========================================================
    // PRINT
    // =========================================================

    private fun printReceipt(
        receipt: String,
        reprint: Boolean
    ) {

        try {

            val manager = getSystemService(
                Context.PRINT_SERVICE
            ) as PrintManager

            val webView = WebView(this)

            val content = if (reprint) {
                "*** REPRINT ***\n\n$receipt"
            } else {
                receipt
            }

            val html = """
                <html>
                <body style="font-family:monospace;color:black;background:white;">
                <pre>${escapeHtml(content)}</pre>
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )

            webView.postDelayed({

                val adapter = webView.createPrintDocumentAdapter(
                    "PROMAX Receipt"
                )

                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(
                        PrintAttributes.Resolution(
                            "default",
                            "Default",
                            300,
                            300
                        )
                    )
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                manager.print(
                    "PROMAX Receipt",
                    adapter,
                    attributes
                )

            }, 1000)

        } catch (e: Exception) {
            toast("Print error: ${e.message}")
        }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    // =========================================================
    // CLEAR CART
    // =========================================================

    private fun clearCartConfirm(tenderBox: EditText) {

        if (cartItems.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Clear Cart?")
            .setMessage("Remove all products from the current sale?")
            .setPositiveButton("CLEAR") { _, _ ->
                cartItems.clear()
                tenderBox.setText("")
                tendered = 0.0
                refreshCart()
                updateReceipt()
                updatePayment()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun cashierName(): String {

        val prefs = getSharedPreferences(
            "pos_prefs",
            MODE_PRIVATE
        )

        return prefs.getString("cashier_name", null)
            ?: prefs.getString("logged_user", null)
            ?: "Cashier"
    }

    private fun newReceiptNumber(): String {
        return "POS" + SimpleDateFormat(
            "yyMMddHHmmssSSS",
            Locale.getDefault()
        ).format(Date())
    }

    private fun money(value: Double): String {
        return "$" + String.format(Locale.US, "%.2f", value)
    }

    private fun makeText(
        value: String,
        size: Int,
        color: Int,
        bold: Boolean
    ): TextView {

        return TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }

            setPadding(5, 5, 5, 5)
        }
    }

    private fun panel(): LinearLayout {

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(white)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 6, 0, 6)
            layoutParams = params
        }
    }

    private fun makeButton(
        label: String,
        color: Int
    ): Button {

        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(white)
            setBackgroundColor(color)
        }
    }

    private fun separator(): View {

        return View(this).apply {
            setBackgroundColor(Color.rgb(225, 225, 225))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}