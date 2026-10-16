package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class AddCustomerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(16,16,16,16)

        root.addView(TextView(this).apply{ text="SMART POS"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#111827")); setPadding(16,14,16,14); setTypeface(null, Typeface.BOLD); textSize=16f })
        root.addView(TextView(this).apply{ text="ADD CUSTOMER"; setTypeface(null, Typeface.BOLD); textSize=14f; setPadding(0,16,0,16) })

        fun makeField(hint:String): EditText {
            val et = EditText(this)
            et.hint=hint
            et.textSize=13f
            et.setBackgroundColor(Color.parseColor("#F3F4F6"))
            et.setPadding(16,20,16,20)
            et.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,12)}
            return et
        }

        val etCode = makeField("Customer CODE - e.g. CUST-01")
        val etName = makeField("Customer Name")
        val etPhone = makeField("Phone Number")
        val etBal = makeField("Balance - e.g. 0")
        etBal.hint="Opening Balance (optional)"

        root.addView(etCode); root.addView(etName); root.addView(etPhone); root.addView(etBal)

        val btnSave = Button(this)
        btnSave.text="SAVE CUSTOMER"
        btnSave.setBackgroundColor(Color.parseColor("#16A34A"))
        btnSave.setTextColor(Color.WHITE)
        btnSave.setTypeface(null, Typeface.BOLD)
        btnSave.setPadding(0,20,0,20)
        btnSave.setOnClickListener{
            val code = etCode.text.toString().trim()
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val balStr = etBal.text.toString().trim()

            if(code.isBlank() || name.isBlank()){
                Toast.makeText(this,"Enter CODE and NAME", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try{
                val pref = getSharedPreferences("customers", Context.MODE_PRIVATE)
                val o = JSONObject()
                o.put("code", code)
                o.put("customer_code", code)
                o.put("name", name)
                o.put("customer_name", name)
                o.put("phone", phone)
                o.put("phone_number", phone)
                o.put("balance", balStr.toFloatOrNull() ?: 0f)
                o.put("bal", balStr.toFloatOrNull() ?: 0f)
                o.put("id", code)

                pref.edit().putString(code, o.toString()).apply()
                Toast.makeText(this,"Customer $name saved!", Toast.LENGTH_SHORT).show()
                finish()
            }catch(e:Exception){
                Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(btnSave)

        root.addView(Button(this).apply {
            text="BACK"; setBackgroundColor(Color.parseColor("#E5E7EB")); setPadding(0,18,0,18)
            setOnClickListener{ finish() }
        })

        setContentView(root)

        // Auto generate code CUST-01, CUST-02
        try{
            val pref = getSharedPreferences("customers", Context.MODE_PRIVATE)
            val count = pref.all.size + 1
            etCode.setText("CUST-%02d".format(count))
        }catch(_:Exception){}
    }
}