package com.smartpos
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.*

class LoginActivity : Activity() {
    private val CONTACT_NUMBER = "+263773996805"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,80,40,40);setBackgroundColor(Color.parseColor("#F1F5F9"))}
        val title=TextView(this).apply{text="SmartPOS Login";textSize=20f;setTypeface(null,Typeface.BOLD)}
        val inputEmail=EditText(this).apply{hint="Email/Phone OR Name"}
        val inputPass=EditText(this).apply{hint="PIN";inputType=129}
        val btnLogin=Button(this).apply{text="Login";setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE)}
        val txtLicense=TextView(this).apply{setPadding(0,20,0,0);textSize=15f;setTypeface(null,Typeface.BOLD)}

        val daysLeft=LicenseManager.getDaysLeft(this)
        val canUse=LicenseManager.canUseApp(this)
        val statusText=LicenseManager.getStatusText(this)

        if(!canUse){
            txtLicense.text="❌ EXPIRED!\nContact: $CONTACT_NUMBER\nCall/Whatsapp/Text - TAP TO CONTACT"
            txtLicense.setTextColor(Color.parseColor("#DC2626"))
            txtLicense.setOnClickListener { openContactOptions() }
            btnLogin.isEnabled=false
            btnLogin.text="EXPIRED - TAP LICENSE TEXT TO CONTACT"
            btnLogin.setBackgroundColor(Color.parseColor("#DC2626"))
        }else if(daysLeft in 1..3){
            txtLicense.text="⚠️ $statusText\n$daysLeft days left! Contact: $CONTACT_NUMBER"
            txtLicense.setTextColor(Color.parseColor("#EA580C"))
        }else{
            txtLicense.text="✅ $statusText"
            txtLicense.setTextColor(Color.parseColor("#16A34A"))
        }

        btnLogin.setOnClickListener{
            if(!LicenseManager.canUseApp(this)){
                Toast.makeText(this,"Trial expired. Contact $CONTACT_NUMBER",Toast.LENGTH_LONG).show()
                openContactOptions()
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

        val btnActivate=Button(this).apply{
            text="🔑 ACTIVATE LICENSE";setBackgroundColor(Color.parseColor("#1D4ED8"));setTextColor(Color.WHITE)
            setOnClickListener{startActivity(Intent(this@LoginActivity,ManageShopsActivity::class.java))}
        }

        // WhatsApp quick button
        val btnWhatsApp=Button(this).apply{
            text="💬 WhatsApp: $CONTACT_NUMBER";setBackgroundColor(Color.parseColor("#25D366"));setTextColor(Color.WHITE)
            setOnClickListener{ openWhatsApp() }
        }

        root.addView(title);root.addView(inputEmail);root.addView(inputPass);root.addView(btnLogin);root.addView(btnActivate);root.addView(btnWhatsApp);root.addView(txtLicense)
        setContentView(root)
    }

    private fun openContactOptions() {
        // Show chooser: Call or WhatsApp
        val options = arrayOf("📞 Call $CONTACT_NUMBER", "💬 WhatsApp", "📱 Text/SMS")
        android.app.AlertDialog.Builder(this)
           .setTitle("Contact Chatewa")
           .setItems(options) { _, which ->
                when(which) {
                    0 -> { val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CONTACT_NUMBER")); startActivity(i) }
                    1 -> openWhatsApp()
                    2 -> { val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$CONTACT_NUMBER")); startActivity(i) }
                }
            }.show()
    }

    private fun openWhatsApp() {
        try {
            val uri = Uri.parse("https://wa.me/263773996805?text=Hi%20Chatewa%2C%20my%20SmartPOS%20license%20expired.%20Help%20me%20renew.")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CONTACT_NUMBER"))
            startActivity(i)
        }
    }
}
