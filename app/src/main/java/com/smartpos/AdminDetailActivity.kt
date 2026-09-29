        } else if(title=="Stock Control"){
            root.addView(tv("STOCK RECEIVING (GRN) - Linked to Suppliers", Color.BLUE, 14f))
            val supNames=suppliersPrefs.all.keys.toTypedArray()
            val supSp=Spinner(this).apply { adapter=ArrayAdapter(this@AdminDetailActivity, android.R.layout.simple_spinner_dropdown_item, if(supNames.isEmpty()) arrayOf("No Supplier - Add First") else supNames) }
            root.addView(lbl("Select Supplier")); root.addView(supSp)

            val grnProd=inp("Product Name")
            val grnQty=inp("Qty Received", InputType.TYPE_CLASS_NUMBER)
            val grnBuy=inp("Buy Price", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            root.addView(lbl("Product")); root.addView(grnProd)
            root.addView(lbl("Quantity")); root.addView(grnQty)
            root.addView(lbl("Buy Price")); root.addView(grnBuy)

            val stockList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,16,0,0) }
            val grnList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,16,0,0) }

            // GREEN FIX: No fun refreshStock() / refreshGrn() — use direct code
            fun doRefreshStock(){
                stockList.removeAllViews()
                if(productsPrefs.all.isEmpty()){
                    stockList.addView(tv("No products", Color.GRAY))
                } else {
                    productsPrefs.all.forEach{ entry-> try{
                        val a=entry.value.toString().split("|")
                        if(a.size>=5){
                            val qty=a[4].toIntOrNull()?:0
                            stockList.addView(tv("• ${a[0]} | Qty: $qty${if(qty<10) " ⚠️ LOW STOCK" else ""}", if(qty<10) Color.RED else Color.BLACK, 14f))
                        }
                    }catch(_:Exception){} }
                }
            }

            fun doRefreshGrn(){
                grnList.removeAllViews()
                if(grnPrefs.all.isEmpty()){
                    grnList.addView(tv("No GRNs", Color.GRAY))
                } else {
                    grnPrefs.all.toList().sortedByDescending{ it.first }.take(10).forEach{ entry->
                        val parts=entry.second.toString().split("|")
                        val display=if(parts.size>=5) "${entry.first}\nSupplier: ${parts[0]}\nProduct: ${parts[1]}\nQty: ${parts[2]}\nBuy: $${parts[3]}\nDate: ${parts[4]}" else "${entry.first}: ${entry.second}"
                        grnList.addView(tv("• $display", Color.DKGRAY, 13f))
                    }
                }
            }

            root.addView(btn("RECEIVE STOCK (GRN)","#EA580C"){
                val productName=grnProd.text.toString().trim()
                val quantity=grnQty.text.toString().toIntOrNull()?:0
                val buyPrice=grnBuy.text.toString().trim()
                if(productName.isEmpty()){ toast("Enter product name"); return@btn }
                if(quantity<=0){ toast("Enter valid quantity"); return@btn }
                if(buyPrice.isEmpty()){ toast("Enter buy price"); return@btn }
                val supplier=if(supNames.isEmpty()) "Unknown" else supNames[supSp.selectedItemPosition]

                var found=false; val productEditor=productsPrefs.edit()
                productsPrefs.all.forEach{ entry-> try{
                    val a=entry.value.toString().split("|")
                    if(a.size>=5 && a[0].equals(productName,true)){
                        val newQty=(a[4].toIntOrNull()?:0)+quantity
                        productEditor.putString(entry.key,"${a[0]}|${a[1]}|${buyPrice.ifEmpty{a[2]}}|${a[3]}|$newQty"); found=true
                    }
                }catch(_:Exception){} }
                productEditor.apply()
                if(!found){
                    val id="P${System.currentTimeMillis()}"; productsPrefs.edit().putString(id,"$productName|General|$buyPrice|$buyPrice|$quantity").apply()
                }
                val grnId="GRN${System.currentTimeMillis()}"; val date=java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US).format(java.util.Date())
                grnPrefs.edit().putString(grnId,"$supplier|$productName|$quantity|$buyPrice|$date").apply()
                log("GRN $grnId: Received $quantity x $productName from $supplier")
                toast("Received $quantity x $productName")
                grnProd.setText(""); grnQty.setText(""); grnBuy.setText("")
                doRefreshStock(); doRefreshGrn()
            })

            root.addView(tv("CURRENT STOCK", Color.parseColor("#1E293B"), 14f)); root.addView(stockList); doRefreshStock()
            root.addView(tv("RECENT GRNs", Color.parseColor("#1E293B"), 14f)); root.addView(grnList); doRefreshGrn()
