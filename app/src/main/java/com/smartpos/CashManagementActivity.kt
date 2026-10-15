package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.print.PrintManager
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CashManagementActivity : Activity() {

    lateinit var tvOpening: TextView
    lateinit var tvCashSales: TextView
    lateinit var tvCard: TextView
    lateinit var tvCashIn: TextView
    lateinit var tvCashOut: TextView
    lateinit var tvExpected: TextView
    lateinit var tvActual: TextView
    lateinit var tvShort: TextView
    var openingFloat = 0f
    var cashSales = 0f
    var cardSales = 0f
    var cashInVal = 0f
    var cashOutVal = 0f
    var actualCounted = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        // HEADER
        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(16,14,16,14)
        header.orientation=LinearLayout.HORIZONTAL
        header.gravity=Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text="💵 Cash Management - TODAY"
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        title.textSize=14f
        val btnBack = Button(this)
        btnBack.text="BACK"
        btnBack.setBackgroundColor(Color.parseColor("#E5E7EB"))
        btnBack.setOnClickListener{ finish() }
        header.addView(title, LinearLayout.LayoutParams(0,-2,1f))
        header.addView(btnBack)
        root.addView(header)

        // DATE + BRANCH - EMPTY USER DATA ONLY!
        val topRow = LinearLayout(this)
        topRow.orientation=LinearLayout.HORIZONTAL
        topRow.setPadding(12,12,12,8)
        topRow.gravity=Gravity.CENTER_VERTICAL
        val tvDate = TextView(this)
        tvDate.text="📅 ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
        tvDate.setTextColor(Color.GRAY)
        tvDate.textSize=12f
        val spBranch = Spinner(this)
        spBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, loadBranchesFinal())
        topRow.addView(tvDate, LinearLayout.LayoutParams(0,-2,1f))
        topRow.addView(spBranch, LinearLayout.LayoutParams(-2,-2,0f))
        root.addView(topRow)

        // OPENING FLOAT - $0 EMPTY!
        val openCard = LinearLayout(this)
        openCard.setBackgroundColor(Color.WHITE)
        openCard.setPadding(12,12,12,12)
        openCard.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,6,12,6)}
        openCard.orientation=LinearLayout.HORIZONTAL
        openCard.gravity=Gravity.CENTER_VERTICAL
        val openLabel = TextView(this); openLabel.text="💵 OPENING FLOAT"; openLabel.setTextColor(Color.GRAY); openLabel.textSize=11f; openLabel.setTypeface(null, Typeface.BOLD)
        tvOpening = TextView(this); tvOpening.text="$0"; tvOpening.textSize=20f; tvOpening.setTypeface(null, Typeface.BOLD); tvOpening.setTextColor(Color.parseColor("#2563EB"))
        val btnEdit = Button(this); btnEdit.text="EDIT"; btnEdit.setBackgroundColor(Color.parseColor("#DBEAFE")); btnEdit.setTextColor(Color.parseColor("#2563EB"))
        btnEdit.setOnClickListener{ editOpening() }
        openCard.addView(openLabel); openCard.addView(tvOpening, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(12,0,12,0)}); openCard.addView(btnEdit)
        root.addView(openCard)

        // GRID - ALL $0 EMPTY!
        fun statCard(title:String, value:String, subColor:Int): LinearLayout {
            val c = LinearLayout(this); c.orientation=LinearLayout.VERTICAL; c.setBackgroundColor(Color.WHITE); c.setPadding(12,12,12,12); c.gravity=Gravity.CENTER; c.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
            val t1=TextView(this); t1.text=title; t1.textSize=10f; t1.setTextColor(Color.GRAY); t1.gravity=Gravity.CENTER; t1.setTypeface(null, Typeface.BOLD)
            val t2=TextView(this); t2.text=value; t2.textSize=18f; t2.setTypeface(null, Typeface.BOLD); t2.gravity=Gravity.CENTER
            c.addView(t1); c.addView(t2); return c
        }

        val row1 = LinearLayout(this); row1.orientation=LinearLayout.HORIZONTAL; row1.setPadding(6,0,6,0)
        val cardSales = statCard("💰 CASH SALES","$0",0); tvCashSales = (cardSales.getChildAt(1) as TextView); tvCashSales.setTextColor(Color.parseColor("#16A34A"))
        val cardCard = statCard("💳 CARD/ECOCASH","$0",0); tvCard = (cardCard.getChildAt(1) as TextView)
        row1.addView(cardSales); row1.addView(cardCard); root.addView(row1)

        val row2 = LinearLayout(this); row2.orientation=LinearLayout.HORIZONTAL; row2.setPadding(6,0,6,0)
        val cardIn = statCard("📥 CASH IN","$0",0); tvCashIn = (cardIn.getChildAt(1) as TextView); tvCashIn.setTextColor(Color.parseColor("#16A34A"))
        val cardOut = statCard("📤 CASH OUT","$0",0); tvCashOut = (cardOut.getChildAt(1) as TextView); tvCashOut.setTextColor(Color.RED)
        row2.addView(cardIn); row2.addView(cardOut); root.addView(row2)

        val row3 = LinearLayout(this); row3.orientation=LinearLayout.HORIZONTAL; row3.setPadding(6,0,6,0)
        val cardExp = statCard("🧮 EXPECTED IN DRAWER","$0",0); tvExpected = (cardExp.getChildAt(1) as TextView); tvExpected.setTextColor(Color.parseColor("#1E293B"))
        val cardAct = LinearLayout(this); cardAct.orientation=LinearLayout.VERTICAL; cardAct.setBackgroundColor(Color.WHITE); cardAct.setPadding(12,12,12,12); cardAct.gravity=Gravity.CENTER; cardAct.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
        cardAct.addView(TextView(this).apply{text="🔢 ACTUAL COUNTED"; textSize=10f; setTextColor(Color.GRAY); gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD)})
        tvActual = TextView(this).apply{text="$0"; textSize=18f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER}; cardAct.addView(tvActual)
        tvShort = TextView(this).apply{text=""; textSize=10f; gravity=Gravity.CENTER; setTextColor(Color.RED)}; cardAct.addView(tvShort)
        row3.addView(cardExp); row3.addView(cardAct); root.addView(row3)

        // ACTION BUTTONS
        val btnRow = LinearLayout(this); btnRow.orientation=LinearLayout.HORIZONTAL; btnRow.setPadding(12,8,12,8)
        fun btn(t:String,c:Int, cb:()->Unit): Button { val b=Button(this); b.text=t; b.setBackgroundColor(c); b.setTextColor(Color.WHITE); b.setTypeface(null, Typeface.BOLD); b.textSize=11f; b.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}; b.setOnClickListener{ cb() }; return b }
        btnRow.addView(btn("+ CASH IN", Color.parseColor("#16A34A")){ addCashIn() })
        btnRow.addView(btn("- CASH OUT", Color.parseColor("#DC2626")){ addCashOut() })
        btnRow.addView(btn("COUNT", Color.parseColor("#2563EB")){ doCount() })
        root.addView(btnRow)

        // Z REPORT WITH PRINTER - FINAL!
        val zBox = LinearLayout(this); zBox.setBackgroundColor(Color.parseColor("#0F172A")); zBox.setPadding(12,12,12,12); zBox.orientation=LinearLayout.VERTICAL; zBox.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,0)}
        val btnZ = Button(this); btnZ.text="📄 CLOSE DAY - Z REPORT"; btnZ.setBackgroundColor(Color.parseColor("#1E293B")); btnZ.setTextColor(Color.WHITE); btnZ.setTypeface(null, Typeface.BOLD)
        btnZ.setOnClickListener{ showZReportDialog() }
        zBox.addView(btnZ)

        // PRINTER OPTIONS ROW - CANCEL + PRINT - VISIBLE ALWAYS!
        val printRow = LinearLayout(this); printRow.orientation=LinearLayout.HORIZONTAL; printRow.setPadding(0,10,0,0)
        val btnCancel = Button(this); btnCancel.text="Cancel"; btnCancel.setBackgroundColor(Color.parseColor("#334155")); btnCancel.setTextColor(Color.WHITE); btnCancel.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,6,0)}
        btnCancel.setOnClickListener{ finish() }
        val btnPrint = Button(this); btnPrint.text="🖨️ PRINT"; btnPrint.setBackgroundColor(Color.parseColor("#2563EB")); btnPrint.setTextColor(Color.WHITE); btnPrint.setTypeface(null, Typeface.BOLD); btnPrint.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,0,0,0)}
        btnPrint.setOnClickListener{ printZReport() }
        printRow.addView(btnCancel); printRow.addView(btnPrint)
        zBox.addView(printRow)
        root.addView(zBox)

        // PAYMENT SPLIT - EMPTY!
        val splitTitle = TextView(this); splitTitle.text="≡ PAYMENT SPLIT TODAY"; splitTitle.setTypeface(null, Typeface.BOLD); splitTitle.textSize=12f; splitTitle.setPadding(12,12,12,6); root.addView(splitTitle)
        val th = LinearLayout(this); th.setBackgroundColor(Color.parseColor("#1E293B")); th.setPadding(8,8,8,8); th.orientation=LinearLayout.HORIZONTAL
        fun thCell(s:String): TextView { val v=TextView(this); v.text=s; v.setTextColor(Color.WHITE); v.setTypeface(null, Typeface.BOLD); v.gravity=Gravity.CENTER; v.layoutParams=LinearLayout.LayoutParams(0,-2,1f); v.textSize=10f; return v }
        th.addView(thCell("TIME")); th.addView(thCell("TYPE")); th.addView(thCell("AMOUNT")); th.addView(thCell("REASON")); root.addView(th)
        val emptyList = TextView(this); emptyList.text="No transactions yet - Add cash in/out"; emptyList.setPadding(24,24,24,24); emptyList.gravity=Gravity.CENTER; emptyList.setTextColor(Color.GRAY); emptyList.setBackgroundColor(Color.WHITE)
        root.addView(emptyList)

        setContentView(root)
        refreshCalculations()
    }

    fun loadBranchesFinal(): List<String>{
        val found = mutableListOf("All..")
        // ONLY USER DATA - NO HARDCODED $120 etc!
        for(pn in listOf("shops","branches","shops_db")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val raw=v.toString().trim()
                    if(raw.startsWith("{")){ try{ val o=JSONObject(raw); val n=o.optString("name",""); if(n.isNotBlank() && !found.contains(n)) found.add(n) }catch(_:Exception){} }
                }
            }catch(_:Exception){}
        }
        return found
    }

    fun refreshCalculations(){
        val expected = openingFloat + cashSales + cashInVal - cashOutVal
        tvExpected.text="$${expected.toInt()}"
        val short = actualCounted - expected
        if(actualCounted==0f){ tvShort.text="" } else { tvShort.text=if(short>=0) "OVER +$${short.toInt()}" else "SHORT -$${Math.abs(short).toInt()}" }
    }

    fun editOpening(){ val et=EditText(this); et.hint="0"; android.app.AlertDialog.Builder(this).setTitle("Opening Float").setView(et).setPositiveButton("Save"){_,_-> openingFloat=et.text.toString().toFloatOrNull()?:0f; tvOpening.text="$${openingFloat.toInt()}"; refreshCalculations() }.show() }
    fun addCashIn(){ val et=EditText(this); et.hint="Amount"; android.app.AlertDialog.Builder(this).setTitle("Cash IN").setView(et).setPositiveButton("Add"){_,_-> cashInVal+=et.text.toString().toFloatOrNull()?:0f; tvCashIn.text="$${cashInVal.toInt()}"; refreshCalculations() }.show() }
    fun addCashOut(){ val et=EditText(this); et.hint="Amount"; android.app.AlertDialog.Builder(this).setTitle("Cash OUT").setView(et).setPositiveButton("Add"){_,_-> cashOutVal+=et.text.toString().toFloatOrNull()?:0f; tvCashOut.text="$${cashOutVal.toInt()}"; refreshCalculations() }.show() }
    fun doCount(){ val et=EditText(this); et.hint="Actual counted"; android.app.AlertDialog.Builder(this).setTitle("Count Drawer").setView(et).setPositiveButton("Save"){_,_-> actualCounted=et.text.toString().toFloatOrNull()?:0f; tvActual.text="$${actualCounted.toInt()}"; refreshCalculations() }.show() }

    fun showZReportDialog(){
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val expected = openingFloat + cashSales + cashInVal - cashOutVal
        val report = "SMART POS - Z REPORT\nDate: $date\n\nOpening: $$openingFloat\nCash Sales: $$cashSales\nCard: $$cardSales\nCash IN: $$cashInVal\nCash OUT: $$cashOutVal\nExpected: $$expected\nActual: $$actualCounted\nDiff: $${actualCounted-expected}"
        android.app.AlertDialog.Builder(this).setTitle("Z REPORT").setMessage(report).setPositiveButton("🖨️ PRINT"){_,_-> printZReport()}.setNegativeButton("Close",null).show()
    }

    fun printZReport(){
        try{
            val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Z-REPORT-${System.currentTimeMillis()}"
            val wv = android.webkit.WebView(this)
            val html = "<html><body><h2>SMART POS Z REPORT</h2><p>${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}</p><hr><p>Opening: $$openingFloat</p><p>Sales: $$cashSales</p><p>IN: $$cashInVal OUT: $$cashOutVal</p><p>Expected: $${openingFloat+cashSales+cashInVal-cashOutVal}</p><p>Actual: $$actualCounted</p><hr></body></html>"
            wv.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
            pm.print(jobName, wv.createPrintDocumentAdapter(jobName), android.print.PrintAttributes.Builder().build())
            Toast.makeText(this,"🖨️ Printing...",Toast.LENGTH_SHORT).show()
        }catch(e:Exception){ Toast.makeText(this,"Print error: ${e.message}",Toast.LENGTH_LONG).show() }
    }
}