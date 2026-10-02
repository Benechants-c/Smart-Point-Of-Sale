package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,80,40,40); setBackgroundColor(Color.parseColor("#F1F5F9")) }

        val title = TextView(this).apply { text="SmartPOS Login"; textSize=20f; setTypeface(null,android.graphics.Typeface.BOLD) }
        val inputEmail = EditText(this).apply { hint="Email / Phone (Owner) OR PIN (Cashier)" }
        val inputPass = EditText(this).apply { hint="Password / PIN (4-6 digits)"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val btnLogin = Button(this).apply { text="Login"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE) }
        val txtLicense = TextView(this).apply { setPadding(0,20,0,0) }

        // Check License
        val daysLeft = LicenseManager.daysLeft(this)
        when {
            LicenseManager.isExpired(this) -> {
                txtLicense.text = "❌ LICENSE EXPIRED! Contact Seller to Reactivate."
                txtLicense.setTextColor(Color.parseColor("#DC2626"))
                btnLogin.isEnabled = false
            }
            LicenseManager.shouldShowWarning(this) -> {
                txtLicense.text = "⚠️ License expires in $daysLeft days! (${LicenseManager.getDaysInCurrentMonth()} day cycle)"
                txtLicense.setTextColor(Color.parseColor("#EA580C"))
            }
            else -> {
                txtLicense.text = "✅ License Active - $daysLeft days left"
                txtLicense.setTextColor(Color.parseColor("#16A34A"))
            }
        }

        btnLogin.setOnClickListener{
            val id = inputEmail.text.toString().trim()
            val pin = inputPass.text.toString().trim()

            if(id.isEmpty() || pin.isEmpty()){ Toast.makeText(this,"Enter ID + PIN",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            // 1. Check Owner (Email/Phone)
            val ownerPref = getSharedPreferences("shop_owner", Context.MODE_PRIVATE)
            val ownerEmail = ownerPref.getString("email","admin@smartpos.com")
            val ownerPhone = ownerPref.getString("phone","")
            val ownerPass = ownerPref.getString("pass","1234")

            if((id==ownerEmail || id==ownerPhone) && pin==ownerPass){
                // Owner login = Admin role
                startActivity(Intent(this, AdminActivity::class.java))
                return@setOnClickListener
            }

            // 2. Check Cashier/Manager (PIN 4-6)
            val usersPref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
            var found = false
            for((k,v) in usersPref.all){
                if(!k.startsWith("user_")) continue
                val parts = v.toString().split("|")
                if(parts.size<3) continue
                val name = parts[0]; val savedPin = parts[1]; val role = parts[2]
                if(id.equals(name,true) && pin==savedPin){
                    found = true
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("ROLE", role)
                    intent.putExtra("USER", name)
                    startActivity(intent)
                    break
                }
            }
            if(!found) Toast.makeText(this,"Wrong ID/PIN",Toast.LENGTH_SHORT).show()
        }

        root.addView(title); root.addView(inputEmail); root.addView(inputPass); root.addView(btnLogin); root.addView(txtLicense)
        setContentView(root)
    }
}
