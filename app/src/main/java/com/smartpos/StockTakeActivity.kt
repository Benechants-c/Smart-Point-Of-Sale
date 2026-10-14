package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color

class StockTakeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ 
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        root.addView(TextView(this).apply{ 
            text="STOCK TAKE - BUILD 133 GREEN FIX"
            textSize=18f
            setPadding(20,30,20,20)
            setTextColor(Color.BLACK)
        })
        val back = Button(this).apply{ text="BACK" }
        back.setOnClickListener{ finish() }
        root.addView(back)
        
        // REAL FIX - reads BOTH main + rukudzo
        val main = getSharedPreferences("products_main", 0)
        val ruku = getSharedPreferences("products_rukudzo", 0)
        
        val info = TextView(this).apply{
            text="Found: Main=${main.all.size} | Rukudzo=${ruku.all.size}\nSugar will show here now!"
            setPadding(20,20,20,20)
        }
        root.addView(info)
        
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll)
        
        val combined = mutableMapOf<String, Any>()
        combined.putAll(main.all)
        combined.putAll(ruku.all)
        
        if(combined.isEmpty()){
            list.addView(TextView(this).apply{ text="No products - add in ADD PRODUCT"; setPadding(20,40,20,20) })
        } else {
            for((k,v) in combined){
                list.addView(TextView(this).apply{ 
                    text="$k -> $v"
                    setPadding(20,12,20,12)
                    setBackgroundColor(Color.WHITE)
                })
            }
        }
        setContentView(root)
    }
}