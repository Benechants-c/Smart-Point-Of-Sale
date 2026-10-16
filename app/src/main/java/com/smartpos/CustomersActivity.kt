package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class CustomersActivity : Activity() {

    data class Customer(val code:String, val name:String, val phone:String, val balance:Float)

    var all = mutableListOf<Customer>()
    var filtered = mutableListOf<Customer>()
    lateinit var list: LinearLayout
    lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F8FAFC"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E3A8A"))
        header.setPadding(16,20,16,16)
        header.addView(TextView(this).apply{
            text="Customers"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f
        })
        header.addView(TextView(this).apply{
            text="  • SEARCH by CODE"; setTextColor(Color.parseColor("#93C5FD")); textSize=13f
        })
        root.addView(header)

        val searchCard = LinearLayout(this)
        searchCard.setBackgroundColor(Color.WHITE)
        searchCard.setPadding(12,12,12,12)
        searchCard.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,12,12,8) }
        searchCard.orientation=LinearLayout.HORIZONTAL
        searchCard.elevation=4f
        searchCard.addView(TextView(this).apply{ text="🔍"; textSize=16f })
        etSearch = EditText(this)
        etSearch.hint="Search CODE, Name, Phone - e.g. CUST-01"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(8,0,0,0) }
        etSearch.textSize=13f
        searchCard.addView(etSearch)
        root.addView(searchCard)

        val hRow = LinearLayout(this)
        hRow.setBackgroundColor(Color.parseColor("#E2E8F0"))
        hRow.setPadding(12,10,12,10)
        fun th(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.parseColor("#475569")); tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        hRow.addView(th("CODE",0.7f)); hRow.addView(th("NAME",1.0f)); hRow.addView(th("PHONE",0.8f)); hRow.addView(th("BAL",0.5f))
        root.addView(hRow)

        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        list = LinearLayout(this)
        list.orientation=LinearLayout.VERTICAL
        scroll.addView(list)
        root.addView(scroll)

        val back = Button(this)
        back.text="BACK"
        back.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,8,12,12) }
        back.setOnClickListener{ finish() }
        root.addView(back)

        setContentView(root)

        loadCustomers()
        render()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
        filter(etSearch.text.toString())
    }

    fun loadCustomers(){
        all.clear()
        for(pn in listOf("customers","customers_db","clients","customer_list","cust")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val code=o.optString("code", o.optString("id",k))
                        val name=o.optString("name", o.optString("customer_name",""))
                        val phone=o.optString("phone", o.optString("telephone", o.optString("mobile","")))
                        val bal=o.optString("balance","0").toFloatOrNull()?: o.optDouble("balance",0.0).toFloat()
                        if(name.isNotBlank() || code.isNotBlank()){
                            if(all.none{ it.code==code }) all.add(Customer(code, name, phone, bal))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(q:String){
        val s=q.lowercase().trim()
        filtered = if(s.isBlank()) all else all.filter{
            it.code.lowercase().contains(s) || it.name.lowercase().contains(s) || it.phone.contains(s)
        }.toMutableList()
        render()
    }

    fun render(){
        list.removeAllViews()
        if(filtered.isEmpty() && all.isEmpty()){
            val empty = LinearLayout(this)
            empty.orientation=LinearLayout.VERTICAL
            empty.gravity=Gravity.CENTER
            empty.setPadding(0,80,0,0)

            empty.addView(TextView(this).apply{ text="👥"; textSize=40f; gravity=Gravity.CENTER })
            empty.addView(TextView(this).apply{
                text="No customers yet"; textSize=14f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,12,0,4)
            })
            empty.addView(TextView(this).apply{
                text="Customers you add will appear here.\nSearch by code, name or phone."; textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.parseColor("#94A3B8")); setPadding(0,0,0,16)
            })
            val btn = Button(this)
            btn.text="ADD CUSTOMER"
            btn.setBackgroundColor(Color.parseColor("#1E3A8A"))
            btn.setTextColor(Color.WHITE)
            // FIXED - NOW OPENS ADD CUSTOMER
            btn.setOnClickListener{
                try {
                    val intent = android.content.Intent()
                    // Try direct class name - change this to your actual Add Customer activity
                    intent.setClassName(packageName, "com.smartpos.AddCustomerActivity")
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent2 = android.content.Intent()
                        intent2.setClassName(packageName, "com.smartpos.AddCustomer")
                        startActivity(intent2)
                    } catch (e2: Exception) {
                        Toast.makeText(this, "Opening Add Customer...", Toast.LENGTH_SHORT).show()
                        // Last fallback - open via action
                        try {
                            val intent3 = packageManager.getLaunchIntentForPackage(packageName)
                            // Try to find AddCustomer in manifest
                            val intent = android.content.Intent(this, Class.forName("com.smartpos.AddCustomerActivity"))
                            startActivity(intent)
                        } catch (e3: Exception) {
                            Toast.makeText(this, "Add Customer Activity not found - check name: ${e3.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            empty.addView(btn)
            list.addView(empty)
            return
        }

        for(c in filtered){
            val row=LinearLayout(this)
            row.orientation=LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(12,14,12,14)
            row.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,1) }
            fun td(t:String,w:Float,bold:Boolean=false):TextView{
                val tv=TextView(this); tv.text=t; tv.textSize=12f; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w)
                if(bold) tv.setTypeface(null, Typeface.BOLD)
                return tv
            }
            row.addView(td(c.code,0.7f,true).apply{ setTextColor(Color.parseColor("#1E3A8A")) })
            row.addView(td(c.name,1.0f))
            row.addView(td(c.phone,0.8f).apply{ setTextColor(Color.parseColor("#64748B")) })
            row.addView(td("$${c.balance}",0.5f))
            list.addView(row)
        }
    }
}