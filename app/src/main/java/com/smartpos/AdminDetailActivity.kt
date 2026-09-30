package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class AdminDetailActivity : Activity() {
    private val grnProducts = mutableListOf<ProductLine>()
    private val keyCounter = AtomicLong(0)
    data class ProductLine(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)

    private fun romLabel(t: String): TextView = TextView(this).apply { text=t.uppercase();textSize=11f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);setBackgroundColor(Color.parseColor("#1E293B"));setPadding(12,8,12,8) }
    private fun inputBox(h: String=""): EditText = EditText(this).apply { hint=h;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun numDec(h: String=""): EditText = EditText(this).apply { hint=h;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun numInt(h: String=""): EditText = EditText(this).apply { hint=h;inputType=InputType.TYPE_CLASS_NUMBER;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun today(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    private fun grnNum(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + "-" + (System.currentTimeMillis()%1000)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title=intent.getStringExtra("TITLE")?:"Admin"

        // ===== BUILD 128 REAL ONLY - CORRECT ORDER - TRANSFER FIRST =====
        if(title.contains("TRANSFER", true)){
            try { startActivity(Intent(this, StockTransferActivity::class.java)) } catch(e: Exception){ Toast.makeText(this, "Transfer: ${e.message}", Toast.LENGTH_LONG).show() }
            finish(); return
        }
        if(title.contains("BRANCH", true) || title.contains("SHOP", true)){
            try { startActivity(Intent(this, BranchesActivity::class.java)) } catch(e: Exception){ Toast.makeText(this, "Branches: ${e.message}", Toast.LENGTH_LONG).show() }
            finish(); return
        }

        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10)}
        val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12)}
        head.addView(TextView(this).apply{text=title.uppercase();setTextColor(Color.WHITE);textSize=16f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        head.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(head)

        val isStock=title.contains("STOCK",true)||title.contains("RECEIVE",true)||title.contains("GRN",true)
        if(isStock){
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F8FAFC"));setPadding(8,8,8,8)}
            card.addView(romLabel("Supplier / Optional"));val sup=inputBox("Supplier");card.addView(sup)
            card.addView(romLabel("Date"));val date=inputBox("").apply{setText(today())};card.addView(date)
            card.addView(romLabel("Invoice No (Optional)"));val inv=inputBox("Invoice");card.addView(inv)
            card.addView(romLabel("GRN No"));var grnNo=grnNum();val grn=inputBox("").apply{setText("GRN-$grnNo");isEnabled=false};card.addView(grn)

            val th=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(6,8,6,8)}
            fun h(t:String):TextView=TextView(this).apply{text=t;textSize=10f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,-2,1f);gravity=Gravity.CENTER}
            th.addView(h("Product"));th.addView(h("Code"));th.addView(h("Cost"));th.addView(h("Sell"));th.addView(h("Qty"));card.addView(th)
            val tableBody=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);minimumHeight=100};card.addView(tableBody)

            fun refreshTable(){
                tableBody.removeAllViews()
                if(grnProducts.isEmpty()){
                    tableBody.addView(TextView(this).apply{text="No products yet - Click ADD PRODUCT below";gravity=Gravity.CENTER;setPadding(0,30,0,30);setTextColor(Color.GRAY);textSize=12f})
                } else {
                    var tq=0;var tc=0.0
                    grnProducts.forEach{p->
                        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(4,6,4,6);setBackgroundColor(Color.WHITE)}
                        fun c(v:String):TextView=TextView(this).apply{text=v;textSize=11f;setTextColor(Color.BLACK);gravity=Gravity.CENTER;setPadding(2,8,2,8);layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                        row.addView(c(p.name));row.addView(c(p.code));row.addView(c("%.2f".format(p.cost)));row.addView(c("%.2f".format(p.sell)));row.addView(c(p.qty.toString()))
                        tableBody.addView(row);tq+=p.qty;tc+=p.cost*p.qty
                    }
                    tableBody.addView(TextView(this).apply{text="TOTAL: ${grnProducts.size} items | Qty: $tq | COST: $${"%.2f".format(tc)}";setBackgroundColor(Color.parseColor("#DBEAFE"));setPadding(12,12,12,12);setTypeface(null,Typeface.BOLD);setTextColor(Color.BLACK)})
                }
            }
            refreshTable()

            card.addView(Button(this).apply{
                text="ADD PRODUCT";setBackgroundColor(Color.parseColor("#22C55E"));setTextColor(Color.WHITE)
                setOnClickListener{
                    val dlg=LinearLayout(this@AdminDetailActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
                    val n=inputBox("");n.hint="Product Name *";dlg.addView(romLabel("Product Name *"));dlg.addView(n)
                    val co=inputBox("");co.hint="Code (or leave N/A)";dlg.addView(romLabel("Code"));dlg.addView(co)
                    val c=numDec("");c.hint="Cost e.g. 5.00";dlg.addView(romLabel("Cost"));dlg.addView(c)
                    val s=numDec("");s.hint="Selling e.g. 8.00";dlg.addView(romLabel("Selling"));dlg.addView(s)
                    val q=numInt("");q.hint="Quantity * e.g. 10";dlg.addView(romLabel("Quantity * REQUIRED"));dlg.addView(q)
                    android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Add Product").setView(dlg)
                     .setPositiveButton("ADD"){_,_->
                            val name=n.text.toString().trim()
                            if(name.isEmpty()){Toast.makeText(this@AdminDetailActivity,"❌ Name required!",Toast.LENGTH_LONG).show();return@setPositiveButton}
                            val code=co.text.toString().trim().ifEmpty{"N/A"}
                            val cost=c.text.toString().trim().toDoubleOrNull()?:0.0
                            val sell=s.text.toString().trim().toDoubleOrNull()?:0.0
                            val qtyStr=q.text.toString().trim()
                            if(qtyStr.isEmpty()){Toast.makeText(this@AdminDetailActivity,"❌ Quantity is REQUIRED! Enter e.g. 10",Toast.LENGTH_LONG).show();return@setPositiveButton}
                            val qty=qtyStr.toIntOrNull()?:0
                            if(qty<=0){Toast.makeText(this@AdminDetailActivity,"❌ Qty must be >0 integer!",Toast.LENGTH_LONG).show();return@setPositiveButton}
                            grnProducts.add(ProductLine(name,code,cost,sell,qty))
                            refreshTable()
                            Toast.makeText(this@AdminDetailActivity,"✅ $name x$qty ADDED!",Toast.LENGTH_LONG).show()
                        }.setNegativeButton("CANCEL",null).show()
                }
            })

            val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,12,0,0)}
            bottom.addView(Button(this).apply{text="SAVE DRAFT";setBackgroundColor(Color.parseColor("#E5E7EB"));layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)};setOnClickListener{
                if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first!",Toast.LENGTH_SHORT).show();return@setOnClickListener}
                try{val j=JSONObject();j.put("grnNo",grnNo);j.put("supplier",sup.text.toString());j.put("date",date.text.toString());j.put("invoice",inv.text.toString());val arr=JSONArray();grnProducts.forEach{p->val o=JSONObject();o.put("name",p.name);o.put("code",p.code);o.put("cost",p.cost);o.put("sell",p.sell);o.put("qty",p.qty);arr.put(o)};j.put("items",arr);getSharedPreferences("grn_drafts",0).edit().putString(grnNo,j.toString()).apply();Toast.makeText(this@AdminDetailActivity,"DRAFT GRN-$grnNo SAVED!",Toast.LENGTH_LONG).show()}catch(e:Exception){Toast.makeText(this@AdminDetailActivity,"Error: ${e.message}",Toast.LENGTH_LONG).show()}
            }})
            bottom.addView(Button(this).apply{text="PRINT GRN";setBackgroundColor(Color.parseColor("#E5E7EB"));layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)};setOnClickListener{
                if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first!",Toast.LENGTH_SHORT).show();return@setOnClickListener}
                var tq=0;var tc=0.0;val sb=StringBuilder();sb.append("GRN RECEIPT\nGRN: GRN-$grnNo\nSupplier: ${sup.text}\nDate: ${date.text}\nInvoice: ${inv.text}\n\nProduct | Code | Cost | Sell | Qty\n");grnProducts.forEach{p->sb.append("${p.name} | ${p.code} | ${p.cost} | ${p.sell} | ${p.qty}\n");tq+=p.qty;tc+=p.cost*p.qty};sb.append("\nTotal: ${grnProducts.size} items, Qty: $tq, COST: $${"%.2f".format(tc)}");val i=Intent(Intent.ACTION_SEND);i.type="text/plain";i.putExtra(Intent.EXTRA_TEXT,sb.toString());startActivity(Intent.createChooser(i,"Share GRN"))
            }})
            bottom.addView(Button(this).apply{text="COMPLETE RECEIVE";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)};setOnClickListener{
                if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first!",Toast.LENGTH_SHORT).show();return@setOnClickListener}
                val prodPrefs=getSharedPreferences("products_db",0);val all=prodPrefs.all;val byCode=mutableMapOf<String,String>();val byName=mutableMapOf<String,String>()
                all.forEach{(k,v)->try{val s=v.toString();var code="";var name="";if(s.trim().startsWith("{")){val j=JSONObject(s);name=j.optString("name");code=j.optString("code")}else{val p=s.split("|");name=p[0];code=p.getOrNull(1)?:""};if(code.isNotEmpty()&&code.uppercase()!="N/A")byCode[code.lowercase()]=k;if(name.isNotEmpty())byName[name.lowercase()]=k}catch(_:Exception){}}
                val ed=prodPrefs.edit();grnProducts.forEachIndexed{idx,p->val isReal=p.code.uppercase()!="N/A"&&p.code.isNotBlank();val ex=if(isReal)byCode[p.code.lowercase()]?:byName[p.name.lowercase()] else byName[p.name.lowercase()];if(ex!=null){try{val old=all[ex].toString();var oq=0;var oc=0.0;var os=0.0;if(old.trim().startsWith("{")){val j=JSONObject(old);oq=j.optInt("qty",0);oc=j.optDouble("cost",0.0);os=j.optDouble("sell",0.0)}else{val pp=old.split("|");oq=pp.getOrNull(4)?.toIntOrNull()?:0;oc=pp.getOrNull(2)?.toDoubleOrNull()?:0.0;os=pp.getOrNull(3)?.toDoubleOrNull()?:0.0};val nq=oq+p.qty;val avg=if(oq>0)((oc*oq)+(p.cost*p.qty))/nq else p.cost;val fs=if(p.sell>0)p.sell else os;val nj=JSONObject();nj.put("name",p.name);nj.put("code",p.code);nj.put("cost",avg);nj.put("sell",fs);nj.put("qty",nq);ed.putString(ex,nj.toString())}catch(_:Exception){}}else{val sc=p.code.replace(" ","_").replace("|","_").ifEmpty{"NA"};val uk="P_${sc}_${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${idx}";val nj=JSONObject();nj.put("name",p.name);nj.put("code",p.code);nj.put("cost",p.cost);nj.put("sell",p.sell);nj.put("qty",p.qty);ed.putString(uk,nj.toString())}};ed.apply();getSharedPreferences("grn_db",0).edit().putString(grnNo,"${sup.text}|${inv.text}|${grnProducts.size}").apply();getSharedPreferences("grn_drafts",0).edit().remove(grnNo).apply();Toast.makeText(this@AdminDetailActivity,"GRN GRN-$grnNo COMPLETED! ${grnProducts.size} items added",Toast.LENGTH_LONG).show();grnProducts.clear();refreshTable();grnNo=grnNum();grn.setText("GRN-$grnNo")
            }})
            card.addView(bottom);root.addView(card)
        } else {
            root.addView(TextView(this).apply{text="Section: $title - Coming soon";setPadding(30,30,30,30);setTextColor(Color.GRAY);gravity=Gravity.CENTER})
        }
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }
}
