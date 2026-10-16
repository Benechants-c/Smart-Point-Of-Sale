package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import org.json.JSONObject

class AddCustomerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.setPadding(16,16,16,16)
        root.addView(TextView(this).apply{text="Add Customer"; setPadding(12,12,12,12); setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)})
        val etCode=EditText(this).apply{hint="CODE e.g. CUST-01"}
        val etName=EditText(this).apply{hint="Name"}
        val etPhone=EditText(this).apply{hint="Phone"}
        val etBal=EditText(this).apply{hint="Balance 0"}
        root.addView(etCode); root.addView(etName); root.addView(etPhone); root.addView(etBal)
        val btn=Button(this).apply{text="SAVE CUSTOMER"; setBackgroundColor(Color.BLUE); setTextColor(Color.WHITE)}
        btn.setOnClickListener{
            val code=etCode.text.toString().trim()
            val name=etName.text.toString().trim()
            if(code.isBlank() || name.isBlank()){ Toast.makeText(this,"Enter code and name",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            try{
                val pref=getSharedPreferences("customers", Context.MODE_PRIVATE)
                val o=JSONObject()
                o.put("code",code); o.put("name",name); o.put("phone",etPhone.text.toString()); o.put("balance",etBal.text.toString().toFloatOrNull()?:0f)
                pref.edit().putString(code, o.toString()).apply()
                Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show()
                finish()
            }catch(e:Exception){ Toast.makeText(this,"Error ${e.message}",Toast.LENGTH_SHORT).show() }
        }
        root.addView(btn)
        root.addView(Button(this).apply{text="BACK"; setOnClickListener{finish()}})
        setContentView(root)
        try{
            val pref=getSharedPreferences("customers", Context.MODE_PRIVATE)
            etCode.setText("CUST-%02d".format(pref.all.size+1))
        }catch(_:Exception){}
    }
}