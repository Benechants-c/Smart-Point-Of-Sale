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

    data class Customer(val code:String, val name:String, val phone:String)

    var allCustomers = mutableListOf<Customer>()
    var filtered = mutableListOf<Customer>()
    lateinit var listContainer: LinearLayout
    lateinit var etSearch: EditText
    lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F8FAFC"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#0F2D6B"))
        header.setPadding(16,20,16,20)
        header.addView(TextView(this).apply{ text="Customers - SEARCH by CODE"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        root.addView(header)

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,8,12,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.addView(TextView(this).apply{ text="🔍"; setPadding(8,0,8,0) })
        etSearch = EditText(this)
        etSearch.hint = "Search CODE, Name, Phone - e.g. CUST-01"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        searchBox.addView(etSearch)
        root.addView(searchBox)

        tvCount = TextView(this)
        tvCount.setPadding(16,8,16,8)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.parseColor("#2563EB"))
        root.addView(tvCount)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        listContainer = LinearLayout(this)
        listContainer.orientation = LinearLayout.VERTICAL
        listContainer.setPadding(12,0,12,0)
        scroll.addView(listContainer)
        root.addView(scroll)

        root.addView(Button(this).apply{
            text="BACK"; setOnClickListener{ finish() }
        })

        setContentView(root)

        loadRealCustomers()
        filter("")

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadRealCustomers(){
        allCustomers.clear()

        // SCAN ALL POSSIBLE CUSTOMER PREFS - REAL USER DATA ONLY
        val prefNames = listOf("customers","customers_main","customer_db","customers_db","cust_list","clients","clients_db","customers_list")

        for(prefName in prefNames){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val raw = v.toString()
                        var code=""; var name=""; var phone=""
                        if(raw.trim().startsWith("{")){
                            val o = JSONObject(raw)
                            code = o.optString("code", o.optString("customer_code", o.optString("id","")))
                            name = o.optString("name", o.optString("customer_name", o.optString("fullName","")))
                            phone = o.optString("phone", o.optString("mobile", o.optString("telephone", o.optString("number",""))))
                        } else {
                            val parts = raw.split("|")
                            if(parts.size>=3){
                                code = parts[0]; name = parts[1]; phone = parts[2]
                            }
                        }
                        if(name.isNotBlank()){
                            if(code.isBlank()) code = "CUST-${(allCustomers.size+1).toString().padStart(2,'0')}"
                            if(!allCustomers.any{ it.phone==phone && phone.isNotBlank() }){
                                allCustomers.add(Customer(code,name,phone))
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(q:String){
        val query = q.trim().lowercase()
        filtered = if(query.isBlank()) allCustomers else allCustomers.filter{
            it.code.lowercase().contains(query) || it.name.lowercase().contains(query) || it.phone.contains(query)
        }.toMutableList()

        render()
    }

    fun render(){
        listContainer.removeAllViews()
        tvCount.text = if(allCustomers.isEmpty()) "No customers yet - Add in Customers screen" else "${filtered.size} / ${allCustomers.size} Customers - REAL DATA"

        if(allCustomers.isEmpty()){
            listContainer.addView(TextView(this).apply{
                text="No customer data found!\n\nGo to Add Customer and create customers first.\nThey will appear here automatically.\n\n(NO FAKE DATA)"
                gravity = Gravity.CENTER
                setPadding(0,40,0,0)
                setTextColor(Color.GRAY)
            })
            return
        }

        for(c in filtered){
            val card = LinearLayout(this)
            card.setBackgroundColor(Color.WHITE)
            card.setPadding(16,12,16,12)
            card.orientation = LinearLayout.VERTICAL
            card.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,8) }
            card.addView(TextView(this).apply{ text="${c.code} | ${c.name} | ${c.phone}"; setTypeface(null, Typeface.BOLD); textSize=13f })
            card.addView(TextView(this).apply{ text="CODE: ${c.code} - Search by CODE works!"; textSize=11f; setTextColor(Color.GRAY) })
            listContainer.addView(card)
        }
    }
}