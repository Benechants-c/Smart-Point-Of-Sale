package com.smartpos
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
class LoginActivity : Activity() {
override fun onCreate(b: Bundle?) {
super.onCreate(b)
val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,80,40,40);setBackgroundColor(Color.parseColor("#F1F5F9"))}
val title=TextView(this).apply{text="SmartPOS Login";textSize=20f;setTypeface(null,Typeface.BOLD)}
val inputEmail=EditText(this).apply{hint="Email/Phone OR Name"}
val inputPass=EditText(this).apply{hint="PIN";inputType=129}
val btnLogin=Button(this).apply{text="Login";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE)}
val txtLicense=TextView(this).apply{setPadding(0,20,0,0)}
val daysLeft=LicenseManager.getDaysLeft(this)
val canUse=LicenseManager.canUseApp(this)
val statusText=LicenseManager.getStatusText(this)
if(!canUse){
txtLicense.text="❌ EXPIRED!\n$statusText\nContact: Chatewa"
txtLicense.setTextColor(Color.parseColor("#DC2626"))
btnLogin.isEnabled=false
btnLogin.text="EXPIRED"
}else if(daysLeft in 1..3){
txtLicense.text="⚠️ $statusText\n$daysLeft days!"
txtLicense.setTextColor(Color.parseColor("#EA580C"))
}else{
txtLicense.text="✅ $statusText"
txtLicense.setTextColor(Color.parseColor("#16A34A"))
}
btnLogin.setOnClickListener{
if(!LicenseManager.canUseApp(this)){
Toast.makeText(this,"Trial expired. Activate in Shops",Toast.LENGTH_LONG).show()
startActivity(Intent(this,ManageShopsActivity::class.java))
return@setOnClickListener
}
val id=inputEmail.text.toString().trim()
val pin=inputPass.text.toString().trim()
if(id.isEmpty()||pin.isEmpty()){Toast.makeText(this,"Enter ID+PIN",Toast.LENGTH_SHORT).show();return@setOnClickListener}
val ownerPref=getSharedPreferences("shop_owner",Context.MODE_PRIVATE)
val ownerEmail=ownerPref.getString("email","admin@smartpos.com")
val ownerPhone=ownerPref.getString("phone","")
val ownerPass=ownerPref.getString("pass","1234")
if((id==ownerEmail||id==ownerPhone||id=="admin")&&pin==ownerPass){
startActivity(Intent(this,AdminActivity::class.java));finish();return@setOnClickListener
}
val usersPref=getSharedPreferences("users_db",Context.MODE_PRIVATE)
var foundRole=""
for((k,v) in usersPref.all){
if(!k.startsWith("user_"))continue
val p=v.toString().split("|")
if(p.size<3)continue
if(id.equals(p[0],true)&&pin==p[1]){foundRole=p[2];break}
}
if(foundRole.isNotEmpty()){
when(foundRole.lowercase()){
"admin","manager","superadmin"->startActivity(Intent(this,AdminActivity::class.java))
else->startActivity(Intent(this,SalesActivity::class.java))
}
finish()
}else Toast.makeText(this,"Wrong ID/PIN",Toast.LENGTH_SHORT).show()
}
val btnActivate=Button(this).apply{text="🔑 ACTIVATE LICENSE";setBackgroundColor(Color.parseColor("#1D4ED8"));setTextColor(Color.WHITE)
setOnClickListener{startActivity(Intent(this@LoginActivity,ManageShopsActivity::class.java))}}
root.addView(title);root.addView(inputEmail);root.addView(inputPass);root.addView(btnLogin);root.addView(btnActivate);root.addView(txtLicense)
setContentView(root)
}
}
