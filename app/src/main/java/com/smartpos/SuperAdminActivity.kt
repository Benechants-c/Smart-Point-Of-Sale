package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class SuperAdminActivity : Activity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var emailBox: EditText
    private lateinit var phoneBox: EditText
    private lateinit var branchesSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0F172A")) }

        val topBar = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(30,40,30,30); orientation=LinearLayout.HORIZONTAL }
        topBar.addView(TextView(this).apply { text=" <- Back"; setTextColor(Color.WHITE); setOnClickListener{ finish() } })
        topBar.addView(TextView(this).apply { text=" SUPER ADMIN - SELLER PANEL"; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#38BDF8")); setPadding(20,0,0,0) })
        root.addView(topBar)

        // Add / Reactivate Client Form
        val form = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
        form.addView(TextView(this).apply { text="Add / Reactivate Client"; setTypeface(null,Typeface.BOLD) })

        emailBox = EditText(this).apply { hint="Client Email (Owner Login)" }
        phoneBox = EditText(this).apply { hint="Client Phone (Owner Login)" }

        branchesSpinner = Spinner(this)
        branchesSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("1 Branch","3 Branches","5 Branches","10 Branches","Unlimited")).apply{ setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val txtInfo = TextView(this).apply {
            val days = LicenseManager.getDaysInCurrentMonth()
            text="Current Month Cycle: $days Days | Warning: 5 Days Before Expiry"; setPadding(0,10,0,10); setTextColor(Color.parseColor("#64748B")); textSize=12f
        }

        val btnReactivate = Button(this).apply { text="✅ REACTIVATE 30/31 DAYS"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
        val btnBlock = Button(this).apply { text="❌ BLOCK CLIENT"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE) }

        btnReactivate.setOnClickListener{
            val email = emailBox.text.toString().trim()
            val phone = phoneBox.text.toString().trim()
            if(email.isEmpty()){ Toast.makeText(this,"Enter client email",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val branches = when(branchesSpinner.selectedItemPosition){
                0->1; 1->3; 2->5; 3->10; else->99
            }

            // Save Owner
            getSharedPreferences("shop_owner", Context.MODE_PRIVATE).edit()
               .putString("email", email)
               .putString("phone", phone)
               .putString("pass", "1234") // default owner pass
               .apply()

            // Reactivate License with 30/31 logic
            LicenseManager.reactivateClient(this, branches)

            Toast.makeText(this,"Client Reactivated! ${LicenseManager.getDaysInCurrentMonth()} Days | $branches Branches",Toast.LENGTH_LONG).show()
            emailBox.text.clear(); phoneBox.text.clear()
            render()
        }

        btnBlock.setOnClickListener{
            getSharedPreferences("license", Context.MODE_PRIVATE).edit().putBoolean("is_active", false).apply()
            Toast.makeText(this,"Client BLOCKED!",Toast.LENGTH_SHORT).show()
            render()
        }

        form.addView(emailBox); form.addView(phoneBox); form.addView(branchesSpinner); form.addView(txtInfo); form.addView(btnReactivate); form.addView(btnBlock)
        root.addView(form)

        // Client List
        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        scroll.addView(listContainer)
        root.addView(scroll)

        setContentView(root)
        render()
    }

    private fun render(){
        listContainer.removeAllViews()
        val ownerPref = getSharedPreferences("shop_owner", Context.MODE_PRIVATE)
        val licensePref = getSharedPreferences("license", Context.MODE_PRIVATE)
        val shopPref = getSharedPreferences("shops_config", Context.MODE_PRIVATE)

        val header = TextView(this).apply { text="CLIENT STATUS"; setTypeface(null,Typeface.BOLD); setPadding(16,12,16,12); setBackgroundColor(Color.parseColor("#E2E8F0")) }
        listContainer.addView(header)

        val email = ownerPref.getString("email","No client")?:"No client"
        val phone = ownerPref.getString("phone","-")?:"-"
        val maxBranches = shopPref.getInt("max_branches", 1)
        val isActive = licensePref.getBoolean("is_active", true)
        val daysLeft = LicenseManager.daysLeft(this)
        val expiry = LicenseManager.getDaysInCurrentMonth()

        val color = if(!isActive || daysLeft<=0) Color.parseColor("#DC2626") else if(daysLeft<=5) Color.parseColor("#EA580C") else Color.parseColor("#16A34A")
        val status = if(!isActive) "BLOCKED" else if(daysLeft<=0) "EXPIRED" else if(daysLeft<=5) "EXPIRING IN $daysLeft DAYS" else "ACTIVE $daysLeft Days"

        val row = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,12,16,12) }
        row.addView(TextView(this).apply { text="Email: $email"; setTypeface(null,Typeface.BOLD) })
        row.addView(TextView(this).apply { text="Phone: $phone" })
        row.addView(TextView(this).apply { text="Branches Allowed: $maxBranches / Cycle: $expiry Days"; setTextColor(Color.parseColor("#64748B")) })
        row.addView(TextView(this).apply { text="Status: $status"; setTextColor(color); setTypeface(null,Typeface.BOLD); textSize=14f })
        listContainer.addView(row)

        // Show users
        val usersPref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
        val uHeader = TextView(this).apply { text="SHOP USERS (Cashier/Manager) - Created by Admin"; setTypeface(null,Typeface.BOLD); setPadding(16,12,16,12); setBackgroundColor(Color.parseColor("#E2E8F0")); setMargins(0,20,0,0) }
        listContainer.addView(uHeader)
        for((k,v) in usersPref.all){
            if(!k.startsWith("user_")) continue
            val parts = v.toString().split("|")
            if(parts.size<4) continue
            listContainer.addView(TextView(this).apply { text="${parts[0]} | ${parts[2]} | ${parts[3]}"; setPadding(16,8,16,8) })
        }
    }

    private fun View.setMargins(l:Int,t:Int,r:Int,b:Int){
        val p = layoutParams as? LinearLayout.LayoutParams?: LinearLayout.LayoutParams(-1,-2)
        p.setMargins(l,t,r,b); layoutParams=p
    }
}
