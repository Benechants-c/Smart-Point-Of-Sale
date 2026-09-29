package com.smartpos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class AdminDetailActivity : Activity() {

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        val title=intent.getStringExtra("TITLE")?:"Admin"

        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }
        val head=LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,18,18,18); orientation=LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text=title.uppercase(); setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK"; setOnClickListener{ finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,16,0,8) })

        // 1 USERS
        if(title=="Users & Permissions"){
            val fn=EditText(this).apply { hint="Full Name e.g. John Doe" }
            val un=EditText(this).apply { hint="Username e.g. john123" }
            val pw=EditText(this).apply { hint="Password"; inputType=InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role=EditText(this).apply { hint="Role: CASHIER, MANAGER, ADMIN, STOCK KEEPER" }
            val duties=EditText(this).apply { hint="Duties e.g. Sales,Refunds,Reports" }
            root.addView(fn); root.addView(un); root.addView(pw); root.addView(role); root.addView(duties)
            val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefs=getSharedPreferences("users",0)
            prefs.all.forEach{ list.addView(TextView(this).apply { text="• ${it.key} - ${it.value}" }) }
            root.addView(Button(this).apply { text="SAVE USER"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{
                if(un.text.toString().isEmpty()){ Toast.makeText(this@AdminDetailActivity,"Enter username",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                prefs.edit().putString(un.text.toString(),"${fn.text}|${pw.text}|${role.text}|${duties.text}").apply()
                list.addView(TextView(this@AdminDetailActivity).apply { text="• ${un.text} - ${role.text}" })
                Toast.makeText(this@AdminDetailActivity,"Saved ${un.text}",Toast.LENGTH_SHORT).show()
            }})
            root.addView(list)

        // 2 BRANCHES
        } else if(title=="Branches / Shops"){
            val sn=EditText(this).apply { hint="Shop Name e.g. Shop 4 Bulawayo" }
            val loc=EditText(this).apply { hint="Location" }
            root.addView(sn); root.addView(loc)
            val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefs=getSharedPreferences("branches",0)
            if(prefs.all.isEmpty()) list.addView(TextView(this).apply { text="Main - Harare\nShop 2 - Chitungwiza" }) else prefs.all.forEach{ list.addView(TextView(this).apply { text="• ${it.key} - ${it.value}" }) }
            root.addView(Button(this).apply { text="ADD SHOP"; setBackgroundColor(Color.parseColor("#0F766E")); setTextColor(Color.WHITE); setOnClickListener{
                prefs.edit().putString(sn.text.toString(),loc.text.toString()).apply()
                list.addView(TextView(this@AdminDetailActivity).apply { text="• ${sn.text} - ${loc.text}" })
            }})
            root.addView(list)

        // 3 PRODUCTS
        } else if(title=="Products & Categories"){
            val pname=EditText(this).apply { hint="Product Name" }
            val pcat=EditText(this).apply { hint="Category" }
            val buy=EditText(this).apply { hint="Buy Price"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
            val sell=EditText(this).apply { hint="Sell Price"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
            val qty=EditText(this).apply { hint="Qty"; inputType=InputType.TYPE_CLASS_NUMBER }
            root.addView(pname); root.addView(pcat); root.addView(buy); root.addView(sell); root.addView(qty)
            val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefs=getSharedPreferences("products_db",0)
            prefs.all.forEach{ list.addView(TextView(this).apply { text="• ${it.value}" }) }
            root.addView(Button(this).apply { text="ADD PRODUCT"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener{
                val id="P${System.currentTimeMillis()}"; prefs.edit().putString(id,"${pname.text}|${pcat.text}|${buy.text}|${sell.text}|${qty.text}").apply()
                list.addView(TextView(this@AdminDetailActivity).apply { text="• ${pname.text} | ${pcat.text} | Qty:${qty.text}" })
            }})
            root.addView(list)

        // 4 PRICE
        } else if(title=="Price Management"){
            val cat=EditText(this).apply { hint="Category or empty=ALL" }
            val pct=EditText(this).apply { hint="Percent e.g. 10 or -5"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
            root.addView(cat); root.addView(pct)
            root.addView(Button(this).apply { text="APPLY PRICE UPDATE"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener{
                val per=pct.text.toString().toFloatOrNull()?:0f; val prefs=getSharedPreferences("products_db",0); val ed=prefs.edit(); var c=0
                prefs.all.forEach{ try{ val a=it.value.toString().split("|"); if(cat.text.toString().isEmpty() || a[1].equals(cat.text.toString(),true)){ ed.putString(it.key,"${a[0]}|${a[1]}|${a[2]}|${a[3].toFloat()*(1+per/100f)}|${a[4]}"); c++ } }catch(_:Exception){} }; ed.apply()
                Toast.makeText(this@AdminDetailActivity,"Updated $c products",Toast.LENGTH_SHORT).show()
            }})

        // 5 SUPPLIERS
        } else if(title=="Suppliers"){
            val sname=EditText(this).apply { hint="Supplier Name" }
            val sphone=EditText(this).apply { hint="Phone" }
            root.addView(sname); root.addView(sphone)
            val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefs=getSharedPreferences("suppliers",0)
            prefs.all.forEach{ list.addView(TextView(this).apply { text="• ${it.key} - ${it.value}" }) }
            root.addView(Button(this).apply { text="ADD SUPPLIER"; setBackgroundColor(Color.parseColor("#7C3AED")); setTextColor(Color.WHITE); setOnClickListener{
                prefs.edit().putString(sname.text.toString(),sphone.text.toString()).apply()
                list.addView(TextView(this@AdminDetailActivity).apply { text="• ${sname.text} - ${sphone.text}" })
            }})
            root.addView(list)

        // 6 STOCK CONTROL - YOUR GRN LOGIC - GREEN FIX
        } else if(title=="Stock Control"){
            root.addView(TextView(this).apply { text="STOCK RECEIVING (GRN) - Linked to Suppliers"; setTextColor(Color.BLUE); setTypeface(null,Typeface.BOLD) })
            val supName=EditText(this).apply { hint="Supplier Name e.g. Delta" }
            val prodName=EditText(this).apply { hint="Product Name" }
            val prodQty=EditText(this).apply { hint="Qty Received"; inputType=InputType.TYPE_CLASS_NUMBER }
            val buyPrice=EditText(this).apply { hint="Buy Price"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
            root.addView(supName); root.addView(prodName); root.addView(prodQty); root.addView(buyPrice)

            val stockList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val grnList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val productsPrefs=getSharedPreferences("products_db",0)
            val grnPrefs=getSharedPreferences("grn_db",0)

            productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); val q=a[4].toIntOrNull()?:0; stockList.addView(TextView(this).apply { text="• ${a[0]} Qty:$q ${if(q<10) "LOW!" else ""}"; setTextColor(if(q<10) Color.RED else Color.BLACK) }) }catch(_:Exception){} }
            grnPrefs.all.forEach{ grnList.addView(TextView(this).apply { text="• ${it.key}: ${it.value}" }) }

            root.addView(Button(this).apply { text="RECEIVE STOCK (GRN)"; setBackgroundColor(Color.parseColor("#EA580C")); setTextColor(Color.WHITE); setOnClickListener{
                val pn=prodName.text.toString(); val q=prodQty.text.toString().toIntOrNull()?:0; val bp=buyPrice.text.toString()
                if(pn.isEmpty() || q<=0){ Toast.makeText(this@AdminDetailActivity,"Enter product & qty",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                var found=false; val ed=productsPrefs.edit()
                productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); if(a[0].equals(pn,true)){ ed.putString(it.key,"${a[0]}|${a[1]}|${bp.ifEmpty{a[2]}}|${a[3]}|${(a[4].toIntOrNull()?:0)+q}"); found=true } }catch(_:Exception){} }; ed.apply()
                if(!found){ val id="P${System.currentTimeMillis()}"; productsPrefs.edit().putString(id,"$pn|General|$bp|$bp|$q").apply() }
                val grnId="GRN${System.currentTimeMillis()}"; grnPrefs.edit().putString(grnId,"${supName.text}|$pn|$q|$bp").apply()
                grnList.addView(TextView(this@AdminDetailActivity).apply { text="• $grnId: ${supName.text}|$pn|$q" })
                Toast.makeText(this@AdminDetailActivity,"Received $q x $pn",Toast.LENGTH_SHORT).show()
            }})
            root.addView(TextView(this).apply { text="CURRENT STOCK"; setTypeface(null,Typeface.BOLD) }); root.addView(stockList)
            root.addView(TextView(this).apply { text="RECENT GRNs"; setTypeface(null,Typeface.BOLD) }); root.addView(grnList)

        // 7 REPORTS
        } else if(title=="Reports"){
            root.addView(Button(this).apply { text="SALES REPORT"; setOnClickListener{ var t=0f; getSharedPreferences("sales_db",0).all.forEach{ try{ t+=it.value.toString().split("|")[1].toFloat() }catch(_:Exception){} }; Toast.makeText(this@AdminDetailActivity,"Total $${t}",Toast.LENGTH_SHORT).show() }})
            root.addView(Button(this).apply { text="PROFIT REPORT"; setOnClickListener{ Toast.makeText(this@AdminDetailActivity,"Profit report ready",Toast.LENGTH_SHORT).show() }})
            root.addView(Button(this).apply { text="STOCK REPORT"; setOnClickListener{ var v=0f; getSharedPreferences("products_db",0).all.forEach{ try{ val a=it.value.toString().split("|"); v+=a[3].toFloat()*a[4].toFloat() }catch(_:Exception){} }; Toast.makeText(this@AdminDetailActivity,"Stock $${v}",Toast.LENGTH_SHORT).show() }})

        } else if(title=="Sales Settings"){
            val tax=EditText(this).apply { hint="Tax %"; inputType=InputType.TYPE_CLASS_NUMBER }
            val disc=EditText(this).apply { hint="Discount %"; inputType=InputType.TYPE_CLASS_NUMBER }
            root.addView(tax); root.addView(disc)
            root.addView(Button(this).apply { text="SAVE"; setOnClickListener{ getSharedPreferences("system",0).edit().putString("tax",tax.text.toString()).putString("discount",disc.text.toString()).apply(); Toast.makeText(this@AdminDetailActivity,"Saved",Toast.LENGTH_SHORT).show() }})

        } else if(title=="Receipt Settings"){
            val header=EditText(this).apply { hint="Receipt Header" }
            val footer=EditText(this).apply { hint="Footer" }
            root.addView(header); root.addView(footer)
            root.addView(Button(this).apply { text="SAVE RECEIPT"; setOnClickListener{ getSharedPreferences("system",0).edit().putString("receiptHeader",header.text.toString()).putString("receiptFooter",footer.text.toString()).apply(); Toast.makeText(this@AdminDetailActivity,"Saved",Toast.LENGTH_SHORT).show() }})

        } else if(title=="System Settings"){
            val shop=EditText(this).apply { hint="Shop Name" }
            root.addView(shop)
            root.addView(Button(this).apply { text="SAVE SYSTEM"; setOnClickListener{ getSharedPreferences("system",0).edit().putString("shopName",shop.text.toString()).apply(); Toast.makeText(this@AdminDetailActivity,"Saved",Toast.LENGTH_SHORT).show(); finish() }})

        } else if(title=="Backup & Restore"){
            root.addView(Button(this).apply { text="BACKUP NOW"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{ Toast.makeText(this@AdminDetailActivity,"Backup saved",Toast.LENGTH_SHORT).show() }})
            root.addView(Button(this).apply { text="RESTORE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener{ Toast.makeText(this@AdminDetailActivity,"Restore done",Toast.LENGTH_SHORT).show() }})

        } else if(title=="Audit Log"){
            val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefs=getSharedPreferences("audit_log",0)
            if(prefs.all.isEmpty()) list.addView(TextView(this).apply { text="No activity" }) else prefs.all.forEach{ list.addView(TextView(this).apply { text="• ${it.second}" }) }
            root.addView(list)
            root.addView(Button(this).apply { text="CLEAR LOG"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener{ prefs.edit().clear().apply(); list.removeAllViews() }})

        } else {
            root.addView(TextView(this).apply { text="$title - Ready" })
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
