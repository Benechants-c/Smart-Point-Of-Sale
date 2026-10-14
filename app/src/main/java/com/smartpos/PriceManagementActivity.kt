package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.app.AlertDialog
import android.text.InputType
import org.json.JSONObject

class PriceManagementActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
        root.addView(TextView(this).apply{ text="Price Management - REAL"; textSize=18f; setPadding(20,20,20,10) })
        val back = Button(this).apply{ text="BACK"; setOnClickListener{ finish() } }
        root.addView(back)
        
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll)
        
        try {
            val pref = getSharedPreferences("products_main", 0)
            val settings = getSharedPreferences("system_settings", 0)
            val cur = settings.getString("currency", "$") ?: "$"
            if(pref.all.isEmpty()){
                list.addView(TextView(this).apply{ text="No products found\nAdd products in Stock"; setPadding(20,40,20,20) })
            } else {
                for((k,v) in pref.all){
                    try{
                        val obj = JSONObject(v.toString())
                        val name = obj.optString("name", k)
                        var price = obj.optString("price", "0")
                        val tv = TextView(this).apply{
                            text="$name - $cur $price (Tap to edit)"
                            textSize=16f
                            setPadding(20,16,20,16)
                            setBackgroundColor(Color.WHITE)
                        }
                        tv.setOnClickListener{
                            val ed = EditText(this).apply{ inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(price) }
                            AlertDialog.Builder(this).setTitle("Edit Price $name").setView(ed)
                                .setPositiveButton("Save"){_,_->
                                    try{
                                        obj.put("price", ed.text.toString())
                                        pref.edit().putString(k, obj.toString()).apply()
                                        tv.text="$name - $cur ${ed.text} (Tap to edit)"
                                        Toast.makeText(this, "Saved", 0).show()
                                    }catch(_:Exception){}
                                }.setNegativeButton("Cancel", null).show()
                        }
                        list.addView(tv)
                        val sep = View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,2) }
                        list.addView(sep)
                    }catch(_:Exception){}
                }
            }
        }catch(e:Exception){
            list.addView(TextView(this).apply{ text="Error: ${e.message}"; setPadding(20,20,20,20) })
        }
        setContentView(root)
    }
}