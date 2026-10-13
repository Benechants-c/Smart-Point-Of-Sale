package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class ManageShopsActivity : Activity() {
private lateinit var shopContainer: LinearLayout
private lateinit var txtStatus: TextView

override fun onCreate(b: Bundle?) {
super.onCreate(b)
val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#F1F5F9"))}

val topBar=LinearLayout(this).apply{setBackgroundColor(Color.parseColor("#1E293B"));setPadding(30,50,30,20);orientation=LinearLayout.VERTICAL}
topBar.addView(TextView(this).apply{text="🏪 Manage Shops - BUILD 131.7 RANDOM";textSize=18f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE)})
txtStatus=TextView(this).apply{text=LicenseManager.getStatusText(this@ManageShopsActivity);setTextColor(Color.parseColor("#38BDF8"));textSize=13f}
topBar.addView(txtStatus)
root.addView(topBar)

val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE)}
val edName=EditText(this).apply{hint="Shop Name e.g. Rumuko Karoi"}
val edLoc=EditText(this).apply{hint="Location e.g. Karoi Town"}
// ✅ FIXED: No more SMART-30 hint, full random only
val edCode=EditText(this).apply{hint="Enter License Code e.g. 8K4P-2M9X"; setBackgroundColor(Color.parseColor("#FEF3C7"))}
val btnAdd=Button(this).apply{text="➕ ADD SHOP";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE)}
val btnActivate=Button(this).apply{text="🔑 ACTIVATE WITH CODE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE)}

btnAdd.setOnClickListener{
val name=edName.text.toString().trim()
if(name.isEmpty()){Toast.makeText(this,"Enter shop name",Toast.LENGTH_SHORT).show();return@setOnClickListener}
val max=LicenseManager.getMaxBranches(this)
val shops=getSharedPreferences("shops_db",Context.MODE_PRIVATE)
if(shops.all.size>=max){
Toast.makeText(this,"❌ LIMIT REACHED! Max $max branches.\n${LicenseManager.getStatusText(this)}\nBuy code: 0773996805",Toast.LENGTH_LONG).show()
return@setOnClickListener
}
val id = name.lowercase().replace(" ","_")
if(shops.contains(id)){
Toast.makeText(this,"❌ Shop ID '$id' already exists!",Toast.LENGTH_LONG).show()
return@setOnClickListener
}
val json = org.json.JSONObject().apply{
    put("id", id)
    put("name", name)
    put("location", edLoc.text.toString())
}.toString()
shops.edit().putString(id, json).apply()
Toast.makeText(this,"✅ Shop $name added ${shops.all.size}/$max",Toast.LENGTH_SHORT).show()
edName.setText(""); edLoc.setText("")
renderShops()
}

btnActivate.setOnClickListener{
val code=edCode.text.toString().trim().uppercase()
if(code.isEmpty()){Toast.makeText(this,"Enter code",Toast.LENGTH_SHORT).show();return@setOnClickListener}
Toast.makeText(this,"⏳ Checking $code online...",Toast.LENGTH_SHORT).show()
btnActivate.isEnabled = false
btnActivate.text = "⏳ CHECKING ONLINE..."

LicenseManager.verifyLicense(this, code) { success, msg ->
    runOnUiThread {
        btnActivate.isEnabled = true
        btnActivate.text = "🔑 ACTIVATE WITH CODE"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        if (success) {
            txtStatus.text = LicenseManager.getStatusText(this)
            edCode.setText("")
            renderShops()
        }
    }
}
}

form.addView(edName);form.addView(edLoc);form.addView(btnAdd)
form.addView(TextView(this).apply{text="--- LICENSE ACTIVATION ---";setTextColor(Color.GRAY);textSize=11f;setPadding(0,20,0,5);gravity=Gravity.CENTER})
// ✅ FIXED: Removed SMART-30 hint completely - now secure message
form.addView(TextView(this).apply{text="Enter code from admin\nOne code = One phone (No sharing)\nContact 0773996805 to buy";setTextColor(Color.parseColor("#2563EB"));textSize=10f;gravity=Gravity.CENTER;setPadding(0