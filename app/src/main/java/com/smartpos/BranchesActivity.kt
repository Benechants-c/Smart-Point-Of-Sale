package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
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
        
        // HEADER - FIXED TO BUILD 129 LICENSED
        val head = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12)}
        head.addView(TextView(this).apply{
            text="BRANCHES / SHOPS - BUILD 129 LICENSED - ${LicenseManager.getStatusText(this@BranchesActivity)}"
            setTextColor(Color.WHITE);textSize=11f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        })
        head.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(head)

        // STATUS BAR WITH DAYS LEFT
        root.addView(TextView(this).apply{
            text = LicenseManager.getStatusText(this@BranchesActivity)
            setPadding(16,8,16,8);setBackgroundColor(Color.parseColor("#F1F5F9"))
            setTextColor(if(LicenseManager.canUseApp(this@BranchesActivity)) Color.parseColor("#16A34A") else Color.RED)
            textSize=12f;setTypeface(null,Typeface.BOLD)
        })

        listLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,10,0,10)}
        root.addView(listLayout)
        
        root.addView(Button(this).apply{
            text="ADD BRANCH / SHOP";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE)
            setOnClickListener{ 
                // LICENSE CHECK
                val max = LicenseManager.getMaxBranches(this@BranchesActivity)
                val count = getShopsPrefs().all.size
                if(count >= max){
                    Toast.makeText(this@BranchesActivity,"❌ LIMIT REACHED! Max $max branches\n${LicenseManager.getStatusText(this@BranchesActivity)}\nActivate license",Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@BranchesActivity, ManageShopsActivity::class.java))
                    return@setOnClickListener
                }
                showAddDialog() 
            }
        })
        root.addView(Button(this).apply{
            text="🔑 ACTIVATE LICENSE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE)
            setOnClickListener{startActivity(Intent(this@BranchesActivity, ManageShopsActivity::class.java))}
        })
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
                val j = JSONObject(value.toString()); val shopId = j.optString("id", id); val name = j.optString("name", shopId); val loc = j.optString("location", ""); val phone = j.optString("phone", ""); val receiptFooter = j.optString("footer", "")
                val row = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F8FAFC"));setPadding(12,8,12,8)}
                
                val topRow = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
                val tv = TextView(this).apply{text="$shopId | $name ${if(loc.isNotEmpty())"($loc)" else ""} ${if(phone.isNotEmpty())"📞$phone" else ""}";setTextColor(Color.BLACK);textSize=13f;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                val del = Button(this).apply{text="DELETE";setBackgroundColor(Color.parseColor("#EF4444"));setTextColor(Color.WHITE);textSize=10f;setOnClickListener{ getShopsPrefs().edit().remove(id).apply(); Toast.makeText(this@BranchesActivity,"Deleted $shopId",Toast.LENGTH_SHORT).show(); refreshList() }}
                val edit = Button(this).apply{text="EDIT";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);textSize=10f;setOnClickListener{ showAddDialog(j) }}
                topRow.addView(tv); topRow.addView(edit); topRow.addView(del)
                
                row.addView(topRow)
                if(receiptFooter.isNotEmpty()){
                    row.addView(TextView(this).apply{text="Receipt: $receiptFooter";textSize=10f;setTextColor(Color.GRAY)})
                }
                listLayout.addView(row)
                listLayout.addView(TextView(this).apply{height=8})
            }catch(_:Exception){}
        }
    }
    
    private fun showAddDialog(existing: JSONObject? = null){
        val isEdit = existing != null
        val dlgLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
        val idInput = EditText(this).apply{hint="ID e.g. karoi_main";setText(existing?.optString("id","")?:"");setTextColor(Color.BLACK);isEnabled=!isEdit}
        val nameInput = EditText(this).apply{hint="Name e.g. Rumuko";setText(existing?.optString("name","")?:"");setTextColor(Color.BLACK)}
        val locInput = EditText(this).apply{hint="Location e.g. Karoi Town";setText(existing?.optString("location","")?:"");setTextColor(Color.BLACK)}
        val phoneInput = EditText(this).apply{hint="Phone for Receipt e.g. 077...";setText(existing?.optString("phone","")?:"");setTextColor(Color.BLACK)}
        val footerInput = EditText(this).apply{hint="Receipt Footer e.g. Thank you!";setText(existing?.optString("footer","")?:"");setTextColor(Color.BLACK)}
        
        dlgLayout.addView(TextView(this).apply{text="Shop ID * (no spaces)";setTypeface(null,Typeface.BOLD)}); dlgLayout.addView(idInput)
        dlgLayout.addView(TextView(this).apply{text="Shop Name *";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(nameInput)
        dlgLayout.addView(TextView(this).apply{text="Location";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(locInput)
        dlgLayout.addView(TextView(this).apply{text="Phone for Receipt *";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(phoneInput)
        dlgLayout.addView(TextView(this).apply{text="Receipt Footer";setTypeface(null,Typeface.BOLD);setPadding(0,10,0,0)}); dlgLayout.addView(footerInput)
        
        android.app.AlertDialog.Builder(this).setTitle(if(isEdit)"Edit Branch/Shop" else "Add Branch/Shop - BUILD 129 LICENSED").setView(dlgLayout)
           .setPositiveButton(if(isEdit)"UPDATE" else "SAVE"){_,_->
                val sid = idInput.text.toString().trim().lowercase().replace(" ","_"); val sname = nameInput.text.toString().trim(); val sloc = locInput.text.toString().trim(); val sphone = phoneInput.text.toString().trim(); val sfooter = footerInput.text.toString().trim()
                if(sid.isEmpty() || sname.isEmpty()){ Toast.makeText(this,"ID and Name REQUIRED!",Toast.LENGTH_LONG).show(); return@setPositiveButton }
                try{
                    val j = JSONObject(); j.put("id", sid); j.put("name", sname); j.put("location", sloc); j.put("phone", sphone); j.put("footer", sfooter)
                    getShopsPrefs().edit().putString(sid, j.toString()).apply()
                    // Also save phone for receipt in shop_owner
                    if(sphone.isNotEmpty()){
                        getSharedPreferences("shop_owner", Context.MODE_PRIVATE).edit().putString("phone", sphone).apply()
                    }
                    Toast.makeText(this,"✅ Branch $sid ${if(isEdit)"UPDATED" else "SAVED"}!",Toast.LENGTH_LONG).show(); refreshList()
                }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show() }
            }.setNegativeButton("CANCEL",null).show()
    }
}
