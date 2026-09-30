package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.InputType
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private fun numInput(h: String=""): EditText = EditText(this).apply { hint=h;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun getToday(): String = SimpleDateFormat("dd/MM/yyyy",Locale.US).format(Date())
    private fun createGrnNumber(): String = "AUTO-%05d".format((System.currentTimeMillis()%100000).toInt())
    private fun htmlEscape(v: String): String = v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title=intent.getStringExtra("TITLE")?:"Admin"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10) }
        val head=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12) }
        head.addView(TextView(this).apply { text=title.uppercase();setTextColor(Color.WHITE);textSize=16f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()} })
        root.addView(head)

        if(title=="Stock Control"||title.contains("RECEIVE",true)){
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F8FAFC"));setPadding(8,8,8,8) }
            card.addView(romLabel("Supplier / Optional"));val sup=inputBox("Supplier");card.addView(sup)
            card.addView(romLabel("Date"));val date=inputBox("").apply{setText(getToday())};card.addView(date)
            card.addView(romLabel("Invoice No (Optional)"));val inv=inputBox("Invoice");card.addView(inv)
            card.addView(romLabel("GRN No"));val grnNo=createGrnNumber();val grn=inputBox("").apply{setText(grnNo);isEnabled=false};card.addView(grn)
            val th=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(6,8,6,8)}
            fun h(t:String):TextView=TextView(this).apply{text=t;textSize=10f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
            th.addView(h("Product"));th.addView(h("Code"));th.addView(h("Cost"));th.addView(h("Sell"));th.addView(h("Qty"));card.addView(th)
            val tableBody=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)};card.addView(tableBody)
            val refresh={tableBody.removeAllViews();if(grnProducts.isEmpty()){tableBody.addView(TextView(this).apply{text="No products - Click ADD PRODUCT";gravity=Gravity.CENTER;setPadding(0,20,0,20);setTextColor(Color.GRAY)})}else{var tq=0;var tc=0.0;grnProducts.forEach{p->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(4,4,4,4)};fun cell(v:String):TextView=TextView(this).apply{text=v;textSize=11f;setTextColor(Color.BLACK);gravity=Gravity.CENTER;setPadding(2,8,2,8);layoutParams=LinearLayout.LayoutParams(0,-2,1f)};row.addView(cell(p.name));row.addView(cell(p.code));row.addView(cell("%.2f".format(p.cost)));row.addView(cell("%.2f".format(p.sell)));row.addView(cell(p.qty.toString()));tableBody.addView(row);tq+=p.qty;tc+=p.cost*p.qty};tableBody.addView(TextView(this).apply{text="Total: ${grnProducts.size} Products Qty: $tq COST: $${"%.2f".format(tc)}";setBackgroundColor(Color.parseColor("#DBEAFE"));setPadding(10,10,10,10);setTypeface(null,Typeface.BOLD)})}};refresh.invoke()
            card.addView(Button(this).apply{text="ADD PRODUCT";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);setOnClickListener{val dlg=LinearLayout(this@AdminDetailActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)};val n=inputBox("").apply{hint="Product Name"};dlg.addView(romLabel("Product Name"));dlg.addView(n);val co=inputBox("").apply{hint="Code"};dlg.addView(romLabel("Code"));dlg.addView(co);val c=numInput("").apply{hint="Cost"};dlg.addView(romLabel("Cost"));dlg.addView(c);val s=numInput("").apply{hint="Selling"};dlg.addView(romLabel("Selling"));dlg.addView(s);val q=numInput("").apply{hint="Quantity"};dlg.addView(romLabel("Quantity"));dlg.addView(q);android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Add Product").setView(dlg).setPositiveButton("ADD"){_,_->val name=n.text.toString().trim();if(name.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Name required",Toast.LENGTH_SHORT).show();return@setPositiveButton};val code=co.text.toString().trim().ifEmpty{"N/A"};val cost=c.text.toString().trim().toDoubleOrNull()?:0.0;val sell=s.text.toString().trim().toDoubleOrNull()?:0.0;val qty=q.text.toString().trim().toIntOrNull()?:0;if(qty<=0){Toast.makeText(this@AdminDetailActivity,"Qty must be >0",Toast.LENGTH_SHORT).show();return@setPositiveButton};grnProducts.add(ProductLine(name,code,cost,sell,qty));refresh.invoke();Toast.makeText(this@AdminDetailActivity,"$name ADDED!",Toast.LENGTH_SHORT).show()}.setNegativeButton("CANCEL",null).show()}})
            val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,10,0,0)}
            bottom.addView(Button(this).apply{text="SAVE DRAFT";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};try{val json=JSONObject();json.put("grnNo",grnNo);json.put("supplier",sup.text.toString().trim());json.put("date",date.text.toString().trim());json.put("invoice",inv.text.toString().trim());val arr=JSONArray();grnProducts.forEach{p->val o=JSONObject();o.put("name",p.name);o.put("code",p.code);o.put("cost",p.cost);o.put("sell",p.sell);o.put("qty",p.qty);arr.put(o)};json.put("items",arr);getSharedPreferences("grn_drafts",0).edit().putString(grnNo,json.toString()).apply();Toast.makeText(this@AdminDetailActivity,"DRAFT $grnNo SAVED! ${grnProducts.size} items",Toast.LENGTH_LONG).show()}catch(e:Exception){Toast.makeText(this@AdminDetailActivity,"Draft error: ${e.message}",Toast.LENGTH_LONG).show()}}})
            bottom.addView(Button(this).apply{text="PRINT GRN";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};var tq=0;var tc=0.0;val html=StringBuilder();html.append("<html><body><h2>GRN RECEIPT</h2><p>GRN: ${htmlEscape(grnNo)}<br>Supplier: ${htmlEscape(sup.text.toString())}<br>Date: ${htmlEscape(date.text.toString())}<br>Invoice: ${htmlEscape(inv.text.toString())}</p><table border='1' cellpadding='6' style='border-collapse:collapse;width:100%'><tr><th>Product</th><th>Code</th><th>Cost</th><th>Sell</th><th>Qty</th></tr>");grnProducts.forEach{p->html.append("<tr><td>${htmlEscape(p.name)}</td><td>${htmlEscape(p.code)}</td><td>${"%.2f".format(p.cost)}</td><td>${"%.2f".format(p.sell)}</td><td>${p.qty}</td></tr>");tq+=p.qty;tc+=p.cost*p.qty};html.append("</table><p>Total: ${grnProducts.size} Qty: $tq COST: $${"%.2f".format(tc)}</p></body></html>");try{val wv=WebView(this@AdminDetailActivity);wv.webViewClient=object:WebViewClient(){override fun onPageFinished(v:WebView?,u:String?){try{val pm=getSystemService(Context.PRINT_SERVICE) as PrintManager;val ad:PrintDocumentAdapter=wv.createPrintDocumentAdapter("GRN_$grnNo");pm.print("GRN_$grnNo",ad,PrintAttributes.Builder().build())}catch(_:Exception){}}};wv.loadDataWithBaseURL(null,html.toString(),"text/HTML","UTF-8",null);Toast.makeText(this@AdminDetailActivity,"Opening printer...",Toast.LENGTH_SHORT).show()}catch(e:Exception){android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("GRN $grnNo").setMessage(html.toString().replace(Regex("<[^>]*>"),"\n")).setPositiveButton("OK",null).show()}}})
            bottom.addView(Button(this).apply{text="COMPLETE RECEIVE";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};val prodPrefs=getSharedPreferences("products_db",0);val all=prodPrefs.all;val byCode=mutableMapOf<String,String>();val byName=mutableMapOf<String,String>();all.forEach{(k,v)->try{val parts=v.toString().split("|");if(parts.size>=5){val code=parts.getOrNull(1)?:"";val name=parts[0];if(code.isNotBlank())byCode[code.lowercase()]=k;byName[name.lowercase()]=k}}catch(_:Exception){}};grnProducts.forEachIndexed{idx,p->val ek=byCode[p.code.lowercase()]?:byName[p.name.lowercase()];if(ek!=null){try{val old=all[ek].toString().split("|");val oldQty=old.getOrNull(4)?.toIntOrNull()?:0;val newQty=oldQty+p.qty;val nv="${p.name}|${p.code}|${p.cost}|${p.sell}|$newQty";prodPrefs.edit().putString(ek,nv).apply()}catch(_:Exception){}}else{val sc=p.code.replace(Regex("[^A-Za-z0-9]"),"_").ifEmpty{"NA"};val uk="P_${sc}_${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${idx}";val nv="${p.name}|${p.code}|${p.cost}|${p.sell}|${p.qty}";prodPrefs.edit().putString(uk,nv).apply()}};getSharedPreferences("grn_db",0).edit().putString(grnNo,"${sup.text}|${inv.text}|${grnProducts.size}").apply();Toast.makeText(this@AdminDetailActivity,"GRN $grnNo COMPLETED! ${grnProducts.size} lines",Toast.LENGTH_LONG).show();grnProducts.clear();refresh.invoke()}})
            card.addView(bottom);root.addView(card)
        }
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }
}
