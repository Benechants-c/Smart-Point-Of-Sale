                    setOnClickListener {

                        if (grnProducts.isEmpty()) {
                            Toast.makeText(
                                this@AdminDetailActivity,
                                "Add products first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        var totalQty = 0
                        var totalCost = 0.0

                        val html = StringBuilder()
                        html.append("<html><body style='font-family:monospace'>")
                        html.append("<h2>GRN RECEIPT</h2>")
                        html.append("<p><b>GRN:</b> ${htmlEscape(grnNo)}<br>")
                        html.append("<b>Supplier:</b> ${htmlEscape(sup.text.toString())}<br>")
                        html.append("<b>Date:</b> ${htmlEscape(date.text.toString())}<br>")
                        html.append("<b>Invoice:</b> ${htmlEscape(inv.text.toString())}</p>")
                        html.append("<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse;width:100%'>")
                        html.append("<tr><th>Product</th><th>Code</th><th>Cost</th><th>Sell</th><th>Qty</th></tr>")

                        grnProducts.forEach { p ->
                            html.append("<tr><td>${htmlEscape(p.name)}</td><td>${htmlEscape(p.code)}</td><td>${"%.2f".format(p.cost)}</td><td>${"%.2f".format(p.sell)}</td><td>${p.qty}</td></tr>")
                            totalQty += p.qty
                            totalCost += p.cost * p.qty
                        }

                        html.append("</table>")
                        html.append("<p><b>Total Products:</b> ${grnProducts.size}<br>")
                        html.append("<b>Total Qty:</b> $totalQty<br>")
                        html.append("<b>TOTAL COST:</b> $${"%.2f".format(totalCost)}</p>")
                        html.append("</body></html>")

                        try {
                            val webView = WebView(this@AdminDetailActivity)
                            webView.webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    try {
                                        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                                        val adapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("GRN_$grnNo")
                                        printManager.print("GRN_$grnNo", adapter, PrintAttributes.Builder().build())
                                    } catch (_: Exception) {}
                                }
                            }
                            webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null)
                            Toast.makeText(this@AdminDetailActivity, "Opening printer...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.app.AlertDialog.Builder(this@AdminDetailActivity)
                               .setTitle("GRN $grnNo")
                               .setMessage(html.toString().replace(Regex("<[^>]*>"), "\n"))
                               .setPositiveButton("OK", null)
                               .show()
                        }
                    }
                }
            )

            // =========================
            // COMPLETE RECEIVE
            // =========================
            bottom.addView(
                Button(this).apply {
                    text = "COMPLETE RECEIVE"
                    setBackgroundColor(Color.parseColor("#2563EB"))
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1.3f).apply { setMargins(4, 0, 0, 0) }

                    setOnClickListener {
                        if (grnProducts.isEmpty()) {
                            Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val prodPrefs = getSharedPreferences("products_db", 0)
                        val all = prodPrefs.all
                        val byCode = mutableMapOf<String, String>()
                        val byName = mutableMapOf<String, String>()

                        all.forEach { (k, v) ->
                            try {
                                val parts = v.toString().split("|")
                                if (parts.size >= 5) {
                                    val code = parts.getOrNull(1)?: ""
                                    val name = parts[0]
                                    if (code.isNotBlank()) byCode[code.lowercase()] = k
                                    byName[name.lowercase()] = k
                                }
                            } catch (_: Exception) {}
                        }

                        grnProducts.forEachIndexed { idx, p ->
                            val existingKey = byCode[p.code.lowercase()]?: byName[p.name.lowercase()]
                            if (existingKey!= null) {
                                try {
                                    val old = all[existingKey].toString().split("|")
                                    val oldQty = old.getOrNull(4)?.toIntOrNull()?: 0
                                    val newQty = oldQty + p.qty
                                    val newVal = "${p.name}|${p.code}|${p.cost}|${p.sell}|$newQty"
                                    prodPrefs.edit().putString(existingKey, newVal).apply()
                                } catch (_: Exception) {}
                            } else {
                                val safeCode = p.code.replace(Regex("[^A-Za-z0-9]"), "_").ifEmpty { "NA" }
                                val uniqueKey = "P_${safeCode}_${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${idx}"
                                val newVal = "${p.name}|${p.code}|${p.cost}|${p.sell}|${p.qty}"
                                prodPrefs.edit().putString(uniqueKey, newVal).apply()
                            }
                        }

                        getSharedPreferences("grn_db", 0).edit()
                           .putString(grnNo, "${sup.text}|${inv.text}|${grnProducts.size}").apply()

                        Toast.makeText(this@AdminDetailActivity, "GRN $grnNo COMPLETED! ${grnProducts.size} lines", Toast.LENGTH_LONG).show()
                        grnProducts.clear()
                        refresh.invoke()
                    }
                }
            )

            card.addView(bottom)
            root.addView(card)
        }

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(root)
            }
        )
    }
}
