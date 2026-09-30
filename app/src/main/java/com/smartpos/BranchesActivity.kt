package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class BranchesActivity : Activity() {
    private lateinit var listLayout: LinearLayout
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10)}
        val head = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12)}
        head.addView(TextView(this).apply{text="BRANCHES / SHOPS - BUILD 128 REAL ONLY";setTextColor(Color.WHITE);textSize=13f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        head.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(head)
        listLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,10,0,10)}
        root.addView(listLayout)
        root.addView(Button(this).apply{text="ADD BRANCH / SHOP";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);setOnClickListener{ showAddDialog() }})
        root.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#E5E7EB"));setOnClickListener{finish()}})
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
        refreshList()
    }
    private fun getShopsPrefs() = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
    private fun refreshList(){
        listLayout.removeAllViews()
        val prefs = getShopsPrefs(); val all = prefs.all
        if(all.isEmpty()){
            listLayout.addView(TextView(this).apply{text="No branches/shops yet. Click ADD BRANCH / SHOP above and SAVE.";setPadding(20,20,20,20);setTextColor(Color.GRAY);gravity=Gravity.CENTER}); return
        }
        all.forEach{(id,value)->
            try{
                val j = JSONObject(value.toString()); val shopId = j.optString("id", id); val name = j.optString("name", shopId); val loc = j.optString("location", "")
                val row = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.parseColor("#F8FAFC"));setPadding(12,8,12,8)}
                val tv = TextView(this).apply{text="$shopId | $name ${if(loc.isNotEmpty())"($loc)" else ""}";setTextColor(Color.BLACK);textSize=13f;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                val del = Button(this).apply{text="DELETE";setBackgroundColor(Color.parseColor("#EF4444"));setTextColor(Color.WHITE);textSize=10f;setOnClickListener{ getShopsPrefs().edit().remove(id).apply(); Toast.makeText(this@BranchesActivity,"Deleted $shopId",Toast.LENGTH_SHORT).show(); refreshList() }}
                row.addView(tv); row.addView(del); listLayout.addView(row)
            }catch(_:Exception){}
        }
    }
    private fun showAddDialog(){
        val dlgLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
        val idInput = EditText(this).apply{hint="ID e.g. karoi_main, chikangwe, gweru";setTextColor(Color.BLACK)}
        val nameInput = EditText(this).apply{hint="Name e.g. Rumuko, Chikangwe Shop";setTextColor(Color.BLACK)}
        val locInput = EditText(this).apply{hint="Location e.g. Karoi Town";setTextColor(Color.BLACK)}
        dlgLayout.addView(TextView(this).apply{text="Shop ID * (no spaces, lowercase)";setTypeface(null,Typeface.BOLD)}); dlgLayout.addView(idInput)
        dlgLayout.addView(TextView(this).apply{text="Shop Name *";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(nameInput)
        dlgLayout.addView(TextView(this).apply{text="Location";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(locInput)
        android.app.AlertDialog.Builder(this).setTitle("Add Branch/Shop - REAL ONLY").setView(dlgLayout)
           .setPositiveButton("SAVE"){_,_->
                val sid = idInput.text.toString().trim().lowercase().replace(" ","_"); val sname = nameInput.text.toString().trim(); val sloc = locInput.text.toString().trim()
                if(sid.isEmpty() || sname.isEmpty()){ Toast.makeText(this,"ID and Name REQUIRED!",Toast.LENGTH_LONG).show(); return@setPositiveButton }
                try{
                    val j = JSONObject(); j.put("id", sid); j.put("name", sname); j.put("location", sloc)
                    getShopsPrefs().edit().putString(sid, j.toString()).apply()
                    Toast.makeText(this,"✅ Branch $sid SAVED!",Toast.LENGTH_LONG).show(); refreshList()
                }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show() }
            }.setNegativeButton("CANCEL",null).show()
    }
}
