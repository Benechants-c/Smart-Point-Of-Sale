package com.smartpos
import android.app.Activity
import android.content.Context
import android.content.Intent
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
topBar.addView(TextView(this).apply{text="🏪 Manage Shops";textSize=18f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE)})
txtStatus=TextView(this).apply{text=LicenseManager.getStatusText(this@ManageShopsActivity);setTextColor(Color.parseColor("#38BDF8"));textSize=13f}
topBar.addView(txtStatus)
root.addView(topBar)
val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE)}
val edName=EditText(this).apply{hint="Shop Name"}
val edLoc=EditText(this).apply{hint="Location"}
val edCode=EditText(this).apply{hint="Enter License Code CHT-..."}
val btnAdd=Button(this).apply{text="➕ ADD SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE)}
val btnActivate=Button(this).apply{text="🔑 ACTIVATE WITH CODE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE)}
val btnSuper=Button(this).apply{text="🔐 SUPER ADMIN";setBackgroundColor(Color.parseColor("#0F172A"));setTextColor(Color.WHITE)}
btnAdd.setOnClickListener{
val name=edName.text.toString().trim()
if(name.isEmpty()){Toast.makeText(this,"Enter shop name",Toast.LENGTH_SHORT).show();return@setOnClickListener}
val max=LicenseManager.getMaxBranches(this)
val shops=getSharedPreferences("shops_config",Context.MODE_PRIVATE)
if(shops.all.size>=max){Toast.makeText(this,"❌ Limit reached! Max $max branches. Buy license.",Toast.LENGTH_LONG).show();return@setOnClickListener}
shops.edit().putString("shop_${System.currentTimeMillis()}", "$name|${edLoc.text}").apply()
Toast.makeText(this,"Shop added",Toast.LENGTH_SHORT).show()
renderShops()
}
btnActivate.setOnClickListener{
val code=edCode.text.toString().trim()
if(LicenseManager.activateLicense(this,code)){
Toast.makeText(this,"✅ License activated!",Toast.LENGTH_LONG).show()
txtStatus.text=LicenseManager.getStatusText(this)
renderShops()
}else Toast.makeText(this,"❌ Invalid code",Toast.LENGTH_SHORT).show()
}
btnSuper.setOnClickListener{startActivity(Intent(this,SuperAdminActivity::class.java))}
form.addView(edName);form.addView(edLoc);form.addView(btnAdd);form.addView(edCode);form.addView(btnActivate);form.addView(btnSuper)
root.addView(form)
shopContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(8,8,8,8)}
val scroll=ScrollView(this);scroll.addView(shopContainer)
root.addView(scroll)
setContentView(root)
renderShops()
}
private fun renderShops(){
shopContainer.removeAllViews()
val prefs=getSharedPreferences("shops_config",Context.MODE_PRIVATE)
if(prefs.all.isEmpty()){
shopContainer.addView(TextView(this).apply{text="No shops yet";setPadding(20,20,20,20);gravity=Gravity.CENTER;setTextColor(Color.GRAY)})
return
}
for((k,v) in prefs.all){
val parts=v.toString().split("|")
val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(12,12,12,12);setBackgroundColor(Color.WHITE)}
row.addView(TextView(this).apply{text=parts[0];setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
val del=Button(this).apply{text="X";setBackgroundColor(Color.parseColor("#FEE2E2"));setTextColor(Color.RED)}
del.setOnClickListener{prefs.edit().remove(k).apply();renderShops()}
row.addView(del)
shopContainer.addView(row)
}
}
}
