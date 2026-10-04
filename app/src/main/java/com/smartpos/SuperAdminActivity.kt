package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
class SuperAdminActivity : Activity() {
override fun onCreate(b: Bundle?) {
super.onCreate(b)
val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.parseColor("#0F172A"))}
val topBar=LinearLayout(this).apply{setBackgroundColor(Color.parseColor("#1E293B"));setPadding(30,40,30,30);orientation=LinearLayout.HORIZONTAL}
topBar.addView(TextView(this).apply{text=" <- Back";setTextColor(Color.WHITE);setOnClickListener{finish()}})
topBar.addView(TextView(this).apply{text=" SUPER ADMIN - CODE GENERATOR";setTypeface(null,Typeface.BOLD);setTextColor(Color.parseColor("#38BDF8"));setPadding(20,0,0,0)})
root.addView(topBar)
val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE)}
val emailBox=EditText(this).apply{hint="Client Email"}
val phoneBox=EditText(this).apply{hint="Client Phone"}
val daysBox=EditText(this).apply{hint="Days e.g. 30, 90, 365 (1 year)";setText("365")}
val branchesSpinner=Spinner(this)
val branchesList=arrayOf("1 Branch","3 Branches","5 Branches","10 Branches","Unlimited (99)")
branchesSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_item,branchesList).apply{setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
val txtInfo=TextView(this).apply{
text="Trial: 14 Days - 1 Branch Only\nNo free after trial | License: Branch-based billing"
setPadding(0,10,0,10);setTextColor(Color.parseColor("#64748B"));textSize=12f
}
val btnReactivate=Button(this).apply{text="✅ GENERATE & ACTIVATE LICENSE CODE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE)}
val btnBlock=Button(this).apply{text="BLOCK CLIENT";setBackgroundColor(Color.parseColor("#DC2626"));setTextColor(Color.WHITE)}
val txtCode=TextView(this).apply{text="Generated code will show here";textSize=14f;setTypeface(null,Typeface.BOLD);setPadding(0,16,0,16);setTextColor(Color.parseColor("#1D4ED8"))}
val listContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)}

btnReactivate.setOnClickListener{
val email=emailBox.text.toString().trim()
val phone=phoneBox.text.toString().trim()
if(email.isEmpty()){Toast.makeText(this,"Enter email",Toast.LENGTH_SHORT).show();return@setOnClickListener}
val days=daysBox.text.toString().toIntOrNull()?:365
val branches=when(branchesSpinner.selectedItemPosition){0->1;1->3;2->5;3->10;else->99}
getSharedPreferences("shop_owner",Context.MODE_PRIVATE).edit().putString("email",email).putString("phone",phone).putString("pass","1234").apply()
// Generate code
val code=LicenseManager.generateCode(branches,days)
LicenseManager.activateLicense(this,code)
txtCode.text="CODE FOR CLIENT:\n$code\n\n$branches branches for $days days\nSend this via WhatsApp"
Toast.makeText(this,"Activated! $branches Branches for $days Days",Toast.LENGTH_LONG).show()
renderList(listContainer)
}

btnBlock.setOnClickListener{
getSharedPreferences("license",Context.MODE_PRIVATE).edit().putBoolean("is_active",false).apply()
getSharedPreferences("license_db",Context.MODE_PRIVATE).edit().putLong("license_expiry",0).putInt("max_branches",0).apply()
Toast.makeText(this,"Client BLOCKED - License cleared",Toast.LENGTH_SHORT).show()
renderList(listContainer)
}

form.addView(emailBox);form.addView(phoneBox);form.addView(daysBox);form.addView(branchesSpinner);form.addView(txtInfo);form.addView(btnReactivate);form.addView(btnBlock);form.addView(txtCode)
root.addView(form)
val scroll=ScrollView(this);scroll.addView(listContainer);root.addView(scroll)
setContentView(root)
renderList(listContainer)
}
private fun renderList(container: LinearLayout){
container.removeAllViews()
val ownerPref=container.context.getSharedPreferences("shop_owner",Context.MODE_PRIVATE)
val email=ownerPref.getString("email","No client")
val maxBranches=LicenseManager.getMaxBranches(container.context)
val daysLeft=LicenseManager.getDaysLeft(container.context)
val canUse=LicenseManager.canUseApp(container.context)
val status=if(!canUse) "BLOCKED/EXPIRED" else if(LicenseManager.isLicensed(container.context)) "LICENSED $daysLeft Days Left" else "TRIAL $daysLeft Days Left"
val color=if(!canUse) Color.parseColor("#DC2626") else if(daysLeft<=3) Color.parseColor("#EA580C") else Color.parseColor("#16A34A")
val row=LinearLayout(container.context).apply{orientation=LinearLayout.VERTICAL;setPadding(16,12,16,12)}
row.addView(TextView(container.context).apply{text="Email: $email";setTypeface(null,Typeface.BOLD)})
row.addView(TextView(container.context).apply{text="Branches Licensed: $maxBranches | ${LicenseManager.getStatusText(container.context)}";setTextColor(Color.parseColor("#64748B"))})
row.addView(TextView(container.context).apply{text="Status: $status";setTextColor(color);setTypeface(null,Typeface.BOLD)})
container.addView(row)
}
}
