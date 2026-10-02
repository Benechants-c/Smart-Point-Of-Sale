package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,80,40,40); setBackgroundColor(Color.parseColor("#F1F5F9")) }

        val title = TextView(this).apply { text="SmartPOS Login"; textSize=20f; setTypeface(null,Typeface.BOLD) }
        val inputEmail = EditText(this).apply { hint="Email / Phone (Owner) OR Name (Cashier)" }
        val inputPass = EditText(this).apply { hint="Password / PIN 4-6"; inputType=129 }
        val btnLogin = Button(this).apply { text="Login"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE) }
        val txtLicense = TextView(this).apply { setPadding(0,20,0,0) }

        // License Check 30/31 days + 5 days warning
        val daysLeft = LicenseManager.daysLeft(this)
        if(LicenseManager.isExpired(this)){
            txtLicense.text="❌ LICENSE EXPIRED! Contact Seller"; txtLicense.setTextColor(Color.parseColor("#DC2626")); btnLogin.isEnabled=false
        } else if(LicenseManager.shouldShowWarning(this)){
            txtLicense.text="⚠️ Expires in $daysLeft days (${LicenseManager.getDaysInCurrentMonth()} day cycle)"; txtLicense.setTextColor(Color.parseColor("#EA580C"))
        } else {
            txtLicense.text="✅ Active - $daysLeft days left"; txtLicense.setTextColor(Color.parseColor("#16A34A"))
        }

        btnLogin.setOnClickListener{
            val id = inputEmail.text.toString().trim()
            val pin = inputPass.text.toString().trim()
            if(id.isEmpty()||pin.isEmpty()){ Toast.makeText(this,"Enter ID+PIN",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val ownerPref = getSharedPreferences("shop_owner", Context.MODE_PRIVATE)
            val ownerEmail = ownerPref.getString("email","admin@smartpos.com")
            val ownerPhone = ownerPref.getString("phone","")
            val ownerPass = ownerPref.getString("pass","1234")

            // Owner login -> ADMIN DASHBOARD (not UsersActivity)
            if((id==ownerEmail || id==ownerPhone || id=="admin") && pin==ownerPass){
                Toast.makeText(this,"Owner Login OK!",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // Cashier/Manager login -> check role
            val usersPref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
            var foundRole = ""
            for((k,v) in usersPref.all){
                if(!k.startsWith("user_")) continue
                val parts=v.toString().split("|")
                if(parts.size<3) continue
                if(id.equals(parts[0],true) && pin==parts[1]){
                    foundRole = parts[2] // Cashier, Manager, Admin
                    break
                }
            }
            if(foundRole.isNotEmpty()){
                Toast.makeText(this,"$id Logged in as $foundRole!",Toast.LENGTH_SHORT).show()
                when(foundRole.lowercase()){
                    "admin", "manager", "superadmin" -> startActivity(Intent(this, AdminActivity::class.java))
                    "cashier" -> startActivity(Intent(this, SalesActivity::class.java))
                    else -> startActivity(Intent(this, SalesActivity::class.java))
                }
                finish()
            } else Toast.makeText(this,"Wrong ID/PIN",Toast.LENGTH_SHORT).show()
        }

        root.addView(title); root.addView(inputEmail); root.addView(inputPass); root.addView(btnLogin); root.addView(txtLicense)
        setContentView(root)
    }
}
