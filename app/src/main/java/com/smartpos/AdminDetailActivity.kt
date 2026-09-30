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

    companion object { const val CURRENCY = "$" } // Change to ZiG / USD / R here

    private val grnProducts = mutableListOf<ProductLine>()
    private val keyCounter = AtomicLong(0)

    data class ProductLine(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)

    private fun romLabel(t: String): TextView = TextView(this).apply { text=t.uppercase();textSize=11f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);setBackgroundColor(Color.parseColor("#1E293B"));setPadding(12,8,12,8) }
    private fun inputBox(h: String=""): EditText = EditText(this).apply { hint=h;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun numInputDecimal(h: String=""): EditText = EditText(this).apply { hint=h;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun numInputInt(h: String=""): EditText = EditText(this).apply { hint=h;inputType=InputType.TYPE_CLASS_NUMBER;setPadding(20,14,20,14);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK) }
    private fun getToday(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    private fun createGrnNumber(): String = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date()) + "-${keyCounter.incrementAndGet()%1000}"
    private fun htmlEscape(v: String): String = v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;")

    // Parse old pipe format OR new JSON format
    private fun parseProductValue(raw: String): Triple<String,String,Array<Double>>? {
        return try {
            if(raw.trim().startsWith("{")) {
                val j = JSONObject(raw); val name=j.optString("name"); val code=j.optString("code");
                val cost=j.optDouble("cost",0.0); val sell=j.optDouble("sell",0.0); val qty=j.optInt("qty",0)
                Triple(name,code,arrayOf(cost,sell,qty.toDouble()))
            } else {
                val p = raw.split("|"); if(p.size<5) return null
                Triple(p[0], p[1], arrayOf(p[2].toDoubleOrNull()?:0.0, p[3].toDoubleOrNull()?:0.0, p[4].toDoubleOrNull()?:0.0))
            }
        } catch (_:Exception){ null }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?:"Admin"
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10) }
        val head = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12) }
        head.addView(TextView(this).apply { text=title.uppercase();setTextColor(Color.WHITE);textSize=16f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()} })
        root.addView(head)

        val isStockScreen = title.equals("Stock Control",true) || title.contains("RECEIVE",true) || title.contains("GRN",true) || title.contains("STOCK",true)

        if(isStockScreen){
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F8FAFC"));setPadding(8,8,8,8) }
            card.addView(romLabel("Supplier / Optional"));val sup=inputBox("Supplier");card.addView(sup)
            card.addView(romLabel("Date"));val date=inputBox("").apply{setText(getToday())};card.addView(date)
            card.addView(romLabel("Invoice No (Optional)"));val inv=inputBox("Invoice");card.addView(inv)
            card.addView(romLabel("GRN No"));var grnNo=createGrnNumber();val grn=inputBox("").apply{setText("GRN-$grnNo");isEnabled=false};card.addView(grn)

            val th=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(6,8,6,8)}
            fun h(t:String):TextView=TextView(this).apply{text=t;textSize=10f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
            th.addView(h("Product"));th.addView(h("Code"));th.addView(h("Cost"));th.addView(h("Sell"));th.addView(h("Qty"));card.addView(th)
            val tableBody=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)};card.addView(tableBody)

            val refresh={tableBody.removeAllViews();if(grnProducts.isEmpty()){tableBody.addView(TextView(this).apply{text="No products - Click ADD PRODUCT";gravity=Gravity.CENTER;setPadding(0,20,0,20);setTextColor(Color.GRAY)})}else{var tq=0;var tc=0.0;grnProducts.forEach{p->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(4,4,4,4)};fun cell(v:String):TextView=TextView(this).apply{text=v;textSize=11f;setTextColor(Color.BLACK);gravity=Gravity.CENTER;setPadding(2,8,2,8);layoutParams=LinearLayout.LayoutParams(0,-2,1f)};row.addView(cell(p.name));row.addView(cell(p.code));row.addView(cell("%.2f".format(p.cost)));row.addView(cell("%.2f".format(p.sell)));row.addView(cell(p.qty.toString()));tableBody.addView(row);tq+=p.qty;tc+=p.cost*p.qty};tableBody.addView(TextView(this).apply{text="Total: ${grnProducts.size} Products Qty: $tq COST: $CURRENCY${"%.2f".format(tc)}";setBackgroundColor(Color.parseColor("#DBEAFE"));setPadding(10,10,10,10);setTypeface(null,Typeface.BOLD)})}};refresh.invoke()

            card.addView(Button(this).apply{text="ADD PRODUCT";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);setOnClickListener{val dlg=LinearLayout(this@AdminDetailActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)};val n=inputBox("").apply{hint="Product Name"};dlg.addView(romLabel("Product Name"));dlg.addView(n);val co=inputBox("").apply{hint="Code"};dlg.addView(romLabel("Code"));dlg.addView(co);val c=numInputDecimal("").apply{hint="Cost"};dlg.addView(romLabel("Cost"));dlg.addView(c);val s=numInputDecimal("").apply{hint="Selling"};dlg.addView(romLabel("Selling"));dlg.addView(s);val q=numInputInt("").apply{hint="Quantity"};dlg.addView(romLabel("Quantity"));dlg.addView(q);android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Add Product").setView(dlg).setPositiveButton("ADD"){_,_->val name=n.text.toString().trim();if(name.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Name required",Toast.LENGTH_SHORT).show();return@setPositiveButton};val rawCode=co.text.toString().trim();val code=rawCode.ifEmpty{"N/A"};val cost=c.text.toString().trim().toDoubleOrNull()?:0.0;val sell=s.text.toString().trim().toDoubleOrNull()?:0.0;val qty=q.text.toString().trim().toIntOrNull()?:0;if(qty<=0){Toast.makeText(this@AdminDetailActivity,"Qty must be >0 (integer)",Toast.LENGTH_SHORT).show();return@setPositiveButton};if(cost<0||sell<0){Toast.makeText(this@AdminDetailActivity,"Price cannot be negative",Toast.LENGTH_SHORT).show();return@setPositiveButton};grnProducts.add(ProductLine(name,code,cost,sell,qty));refresh.invoke();Toast.makeText(this@AdminDetailActivity,"$name ADDED!",Toast.LENGTH_SHORT).show()}.setNegativeButton("CANCEL",null).show()}})

            val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,10,0,0)}
            // SAVE DRAFT - JSON safe
            bottom.addView(Button(this).apply{text="SAVE DRAFT";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};try{val json=JSONObject();json.put("grnNo",grnNo);json.put("supplier",sup.text.toString().trim());json.put("date",date.text.toString().trim());json.put("invoice",inv.text.toString().trim());val arr=JSONArray();grnProducts.forEach{p->val o=JSONObject();o.put("name",p.name);o.put("code",p.code);o.put("cost",p.cost);o.put("sell",p.sell);o.put("qty",p.qty);arr.put(o)};json.put("items",arr);getSharedPreferences("grn_drafts",0).edit().putString(grnNo,json.toString()).apply();Toast.makeText(this@AdminDetailActivity,"DRAFT GRN-$grnNo SAVED!",Toast.LENGTH_LONG).show()}catch(e:Exception){Toast.makeText(this@AdminDetailActivity,"Draft error: ${e.message}",Toast.LENGTH_LONG).show()}}})

            // LOAD DRAFTS - NEW
            bottom.addView(Button(this).apply{text="DRAFTS";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);layoutParams=LinearLayout.LayoutParams(0,-2,0.7f).apply{setMargins(0,0,4,0)};setOnClickListener{val draftsPrefs=getSharedPreferences("grn_drafts",0).all;if(draftsPrefs.isEmpty()){Toast.makeText(this@AdminDetailActivity,"No drafts",Toast.LENGTH_SHORT).show();return@setOnClickListener};val items=draftsPrefs.keys.toTypedArray();android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Load Draft (${items.size})").setItems(items){_,which->try{val key=items[which];val raw=getSharedPreferences("grn_drafts",0).getString(key,"")?:return@setItems;val j=JSONObject(raw);sup.setText(j.optString("supplier"));date.setText(j.optString("date"));inv.setText(j.optString("invoice"));grnNo=key;grn.setText("GRN-$key");val arr=j.optJSONArray("items");grnProducts.clear();if(arr!=null) for(i in 0 until arr.length()){val o=arr.getJSONObject(i);grnProducts.add(ProductLine(o.optString("name"),o.optString("code"),o.optDouble("cost"),o.optDouble("sell"),o.optInt("qty")))};refresh.invoke();Toast.makeText(this@AdminDetailActivity,"Draft $key loaded",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this@AdminDetailActivity,"Load error: ${e.message}",Toast.LENGTH_LONG).show()}}.setPositiveButton("CLOSE",null).setNegativeButton("CLEAR ALL"){_,_->getSharedPreferences("grn_drafts",0).edit().clear().apply();Toast.makeText(this@AdminDetailActivity,"All drafts cleared",Toast.LENGTH_SHORT).show()}.show()}})

            // PRINT GRN
            bottom.addView(Button(this).apply{text="PRINT GRN";setBackgroundColor(Color.parseColor("#E5E7EB"));setTextColor(Color.BLACK);layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};var tq=0;var tc=0.0;val html=StringBuilder();html.append("<html><body><h2>GRN RECEIPT</h2><p>GRN: GRN-${htmlEscape(grnNo)}<br>Supplier: ${htmlEscape(sup.text.toString())}<br>Date: ${htmlEscape(date.text.toString())}<br>Invoice: ${htmlEscape(inv.text.toString())}</p><table border='1' cellpadding='6' style='border-collapse:collapse;width:100%'><tr><th>Product</th><th>Code</th><th>Cost</th><th>Sell</th><th>Qty</th></tr>");grnProducts.forEach{p->html.append("<tr><td>${htmlEscape(p.name)}</td><td>${htmlEscape(p.code)}</td><td>$CURRENCY${"%.2f".format(p.cost)}</td><td>$CURRENCY${"%.2f".format(p.sell)}</td><td>${p.qty}</td></tr>");tq+=p.qty;tc+=p.cost*p.qty};html.append("</table><p>Total Products: ${grnProducts.size}<br>Total Qty: $tq<br>TOTAL COST: $CURRENCY${"%.2f".format(tc)}</p></body></html>");try{val wv=WebView(this@AdminDetailActivity);wv.webViewClient=object:WebViewClient(){override fun onPageFinished(v:WebView?,u:String?){try{val pm=getSystemService(Context.PRINT_SERVICE) as PrintManager;val ad:PrintDocumentAdapter=wv.createPrintDocumentAdapter("GRN_$grnNo");pm.print("GRN_$grnNo",ad,PrintAttributes.Builder().build())}catch(_:Exception){}}};wv.loadDataWithBaseURL(null,html.toString(),"text/HTML","UTF-8",null);Toast.makeText(this@AdminDetailActivity,"Opening printer...",Toast.LENGTH_SHORT).show()}catch(e:Exception){android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("GRN $grnNo").setMessage(html.toString()).setPositiveButton("OK",null).show()}}})

            // COMPLETE RECEIVE - FIXED LOGIC
            bottom.addView(Button(this).apply{text="COMPLETE RECEIVE";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)};setOnClickListener{if(grnProducts.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Add products first",Toast.LENGTH_SHORT).show();return@setOnClickListener};val prodPrefs=getSharedPreferences("products_db",0);val editor=prodPrefs.edit();val liveMap=prodPrefs.all.toMutableMap() as MutableMap<String,Any>;val byCode=mutableMapOf<String,String>();val byName=mutableMapOf<String,String>();liveMap.forEach{(k,v)->parseProductValue(v.toString())?.let{tri->val name=tri.first;val code=tri.second;if(code.isNotBlank()&&code.uppercase()!="N/A")byCode[code.lowercase()]=k;byName[name.lowercase()]=k}};var updated=0;var added=0;grnProducts.forEachIndexed{idx,p->val isRealCode=p.code.isNotBlank()&&p.code.uppercase()!="N/A";val existingKey=if(isRealCode) byCode[p.code.lowercase()]?:byName[p.name.lowercase()] else byName[p.name.lowercase()];if(existingKey!=null){val parsed=parseProductValue(liveMap[existingKey].toString());val oldQty=(parsed?.third?.get(2)?.toInt()?:0);val oldCost=parsed?.third?.get(0)?:0.0;val oldSell=parsed?.third?.get(1)?:0.0;val newQty=oldQty+p.qty;val avgCost=if(oldQty>0&&oldCost>0) ((oldCost*oldQty)+(p.cost*p.qty))/newQty else p.cost;val finalSell=if(p.sell>0) p.sell else oldSell;val j=JSONObject();j.put("name",p.name);j.put("code",p.code);j.put("cost",avgCost);j.put("sell",finalSell);j.put("qty",newQty);editor.putString(existingKey,j.toString());liveMap[existingKey]=j.toString();updated++}else{val safeCode=p.code.replace(Regex("[^A-Za-z0-9]"),"_").ifEmpty{"NA"};val uniqueKey="P_${safeCode}_${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${idx}";val j=JSONObject();j.put("name",p.name);j.put("code",p.code);j.put("cost",p.cost);j.put("sell",p.sell);j.put("qty",p.qty);editor.putString(uniqueKey,j.toString());liveMap[uniqueKey]=j.toString();if(isRealCode)byCode[p.code.lowercase()]=uniqueKey;byName[p.name.lowercase()]=uniqueKey;added++}};editor.apply();getSharedPreferences("grn_db",0).edit().putString(grnNo,"${sup.text}|${inv.text}|${grnProducts.size}|${System.currentTimeMillis()}").apply();getSharedPreferences("grn_drafts",0).edit().remove(grnNo).apply();Toast.makeText(this@AdminDetailActivity,"GRN COMPLETED! $added new, $updated updated. Draft removed.",Toast.LENGTH_LONG).show();grnProducts.clear();refresh.invoke();grnNo=createGrnNumber();grn.setText("GRN-$grnNo")}})

            card.addView(bottom);root.addView(card)
        } else {
            root.addView(TextView(this).apply{text="Section: $title\nThis Admin section is coming soon.\n\nUse Stock Control for GRN receive.";setPadding(30,30,30,30);textSize=14f;setTextColor(Color.GRAY);gravity=Gravity.CENTER})
        }
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }
}
