package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class ManageShopsActivity : Activity() {
private lateinit var shopContainer: LinearLayout
private lateinit var txtStatus: TextView

override fun onCreate(b: Bundle?) {
super.onCreate(b)
val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F1F5F9"))}

val topBar=LinearLayout(this).apply{setBackgroundColor(Color.parseColor("#1E293B"));setPadding(30,50,30,20);orientation=LinearLayout.VERTICAL}
topBar.addView(TextView(this).apply{text="🏪 Manage Shops - BUILD 129";textSize=18f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE)})
txtStatus=TextView(this).apply{text=LicenseManager.getStatusText(this@ManageShopsActivity);setTextColor(Color.parseColor("#38BDF8"));textSize=13f}
topBar.addView(txtStatus)
root.addView(topBar)

val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE)}
val edName=EditText(this).apply{hint="Shop Name e.g. Rumuko Karoi"}
val edLoc=EditText(this).apply{hint="Location e.g. Karoi Town"}
val edCode=EditText(this).apply{hint="Enter License Code CHT-3-30-XXX"; setBackgroundColor(Color.parseColor("#FEF3C7"))}
val btnAdd=Button(this).apply{text="➕ ADD SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE)}
val btnActivate=Button(this).apply{text="🔑 ACTIVATE WITH CODE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE)}

btnAdd.setOnClickListener{
val name=edName.text.toString().trim()
if(name.isEmpty()){Toast.makeText(this,"Enter shop name",Toast.LENGTH_SHORT).show();return@setOnClickListener}
val max=LicenseManager.getMaxBranches(this)
// USE SAME PREF AS BRANCHES ACTIVITY - FIXED
val shops=getSharedPreferences("shops_db",Context.MODE_PRIVATE)
if(shops.all.size>=max){
Toast.makeText(this,"❌ LIMIT REACHED! Max $max branches.\n${LicenseManager.getStatusText(this)}\nBuy code from developer.",Toast.LENGTH_LONG).show()
return@setOnClickListener
}
val json = org.json.JSONObject().apply{
    put("id", name.lowercase().replace(" ","_"))
    put("name", name)
    put("location", edLoc.text.toString())
}.toString()
shops.edit().putString(name.lowercase().replace(" ","_"), json).apply()
Toast.makeText(this,"✅ Shop $name added ${shops.all.size}/$max",Toast.LENGTH_SHORT).show()
edName.setText(""); edLoc.setText("")
renderShops()
}

btnActivate.setOnClickListener{
val code=edCode.text.toString().trim().uppercase()
if(code.isEmpty()){Toast.makeText(this,"Enter code",Toast.LENGTH_SHORT).show();return@setOnClickListener}
if(LicenseManager.activateLicense(this,code)){
Toast.makeText(this,"✅ Activated! ${LicenseManager.getStatusText(this)}",Toast.LENGTH_LONG).show()
txtStatus.text=LicenseManager.getStatusText(this)
edCode.setText("")
renderShops()
}else Toast.makeText(this,"❌ Invalid code. Contact developer",Toast.LENGTH_SHORT).show()
}

// NO SUPER ADMIN BUTTON - REMOVED FOR CLIENT SAFETY

form.addView(edName);form.addView(edLoc);form.addView(btnAdd)
form.addView(TextView(this).apply{text="--- LICENSE ACTIVATION ---";setTextColor(Color.GRAY);textSize=11f;setPadding(0,20,0,5);gravity=Gravity.CENTER})
form.addView(edCode);form.addView(btnActivate)
root.addView(form)

shopContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(8,8,8,8)}
val scroll=ScrollView(this);scroll.addView(shopContainer)
root.addView(scroll)

root.addView(Button(this).apply{text="⬅️ BACK TO BRANCHES";setBackgroundColor(Color.parseColor("#E5E7EB"));setOnClickListener{finish()}})

setContentView(root)
renderShops()
}

private fun renderShops(){
shopContainer.removeAllViews()
val prefs=getSharedPreferences("shops_db",Context.MODE_PRIVATE)
if(prefs.all.isEmpty()){
shopContainer.addView(TextView(this).apply{text="No shops yet";setPadding(20,20,20,20);gravity=Gravity.CENTER;setTextColor(Color.GRAY)})
return
}
for((k,v) in prefs.all){
try{
val j = org.json.JSONObject(v.toString())
val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(12,12,12,12);setBackgroundColor(Color.WHITE)}
row.addView(TextView(this).apply{text=j.optString("name","$k");setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
val del=Button(this).apply{text="X";setBackgroundColor(Color.parseColor("#FEE2E2"));setTextColor(Color.RED)}
del.setOnClickListener{prefs.edit().remove(k).apply();renderShops(); txtStatus.text=LicenseManager.getStatusText(this@ManageShopsActivity)}
row.addView(del)
shopContainer.addView(row)
shopContainer.addView(TextView(this).apply{height=6})
}catch(_:Exception){}
}
txtStatus.text=LicenseManager.getStatusText(this@ManageShopsActivity)
}
}
