package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class ManageShopsActivity : Activity() {

    private lateinit var shopContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(12,12,12,12) }

        root.addView(TextView(this).apply {
            text="🏪 Manage Shops - Receipt Details\n${LicenseManager.getStatusText(this@ManageShopsActivity)}"
            textSize=14f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,4)
            setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(12,10,12,10)
        })

        // LICENSE BAR
        val licenseBar = TextView(this).apply {
            val left = LicenseManager.getDaysLeft(this@ManageShopsActivity)
            val licensed = LicenseManager.isLicensed(this@ManageShopsActivity)
            text = if(licensed) "✅ LICENSED: ${LicenseManager.getMaxBranches(this@ManageShopsActivity)} branches - $left days left"
                   else if(left>0) "⏳ TRIAL: $left days left - 1 branch only - NO FREE AFTER TRIAL"
                   else "❌ EXPIRED - Contact Developer to Activate"
            textSize=12f; setTypeface(null,Typeface.BOLD)
            setBackgroundColor(if(licensed || left>0) Color.parseColor("#DCFCE7") else Color.parseColor("#FEE2E2"))
            setTextColor(if(licensed || left>0) Color.parseColor("#166534") else Color.parseColor("#991B1B"))
            setPadding(12,8,12,8)
            gravity=Gravity.CENTER
        }
        root.addView(licenseBar)

        val btnAdd = Button(this).apply {
            text="➕ ADD NEW SHOP"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setPadding(0,20,0,20); textSize=16f
            setOnClickListener { tryAddBranch() }
        }
        root.addView(btnAdd)

        val btnActivate = Button(this).apply {
            text="🔑 ACTIVATE LICENSE (Enter code from Developer)"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); setPadding(0,16,0,16)
            setOnClickListener { showActivationDialog() }
        }
        root.addView(btnActivate)

        shopContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,12,0,0) }
        val scroll = ScrollView(this).apply { addView(shopContainer) }
        root.addView(scroll)

        setContentView(root)
        refreshShops()
    }

    private fun tryAddBranch(){
        val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
        val currentCount = pref.all.size
        val maxAllowed = LicenseManager.getMaxBranches(this)

        if(!LicenseManager.canUseApp(this)){
            Toast.makeText(this,"❌ TRIAL EXPIRED (${LicenseManager.getTrialDaysLeft(this)} days). Contact Developer: Chatewa",Toast.LENGTH_LONG).show()
            showActivationDialog(); return
        }

        if(currentCount >= maxAllowed){
            val msg = if(LicenseManager.isTrialActive(this) && !LicenseManager.isLicensed(this)){
                "❌ Trial allows 1 branch only ($currentCount/$maxAllowed). Buy license to add more!"
            } else {
                "❌ Branch limit reached ($currentCount/$maxAllowed). You have ${LicenseManager.getMaxBranches(this)} branches licensed. Buy more!"
            }
            Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
            showActivationDialog(); return
        }
        showAddEditDialog(null, null)
    }

    private fun showActivationDialog(){
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,20,28,20); setBackgroundColor(Color.WHITE) }
        lay.addView(TextView(this).apply { text="🔑 Activate License"; textSize=18f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER })
        lay.addView(TextView(this).apply { 
            text="${LicenseManager.getStatusText(this@ManageShopsActivity)}\n\nEnter code from Developer:\nFormat: CHT-3-365-XXXX\n3 = branches, 365 = days"; 
            textSize=12f; setPadding(0,10,0,10); setTextColor(Color.GRAY) 
        })
        val edCode = EditText(this).apply { hint="e.g. CHT-2-365-AF23"; setPadding(20,16,20,16); setBackgroundColor(Color.parseColor("#F1F5F9")) }
        lay.addView(edCode)
        val btn = Button(this).apply { text="ACTIVATE"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            setOnClickListener {
                if(LicenseManager.activateLicense(this@ManageShopsActivity, edCode.text.toString())){
                    Toast.makeText(this@ManageShopsActivity,"✅ LICENSE ACTIVATED! ${LicenseManager.getMaxBranches(this@ManageShopsActivity)} branches for ${LicenseManager.getDaysLeft(this@ManageShopsActivity)} days",Toast.LENGTH_LONG).show()
                    recreate()
                } else {
                    Toast.makeText(this@ManageShopsActivity,"❌ Invalid code. Check and try again.",Toast.LENGTH_SHORT).show()
                }
            }
        }
        lay.addView(btn)
        val d = Dialog(this); d.setContentView(lay); d.show()
        d.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(),-2)
    }

    private fun refreshShops(){
        shopContainer.removeAllViews()
        val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE)

        if(pref.all.isEmpty()){
            shopContainer.addView(TextView(this).apply { text="No shops yet.\nTrial = 1 branch for 14 days only.\nNo free shop after trial."; gravity=Gravity.CENTER; setPadding(0,40,0,0) })
            return
        }

        for((shopId, raw) in pref.all){
            val parts = raw.toString().split("|")
            val name = parts.getOrNull(0)?: shopId
            val address = parts.getOrNull(1)?: ""
            val phone = parts.getOrNull(2)?: ""
            val tin = parts.getOrNull(3)?: ""
            val footer = parts.getOrNull(4)?: "Thank you!"

            val card = LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
                setPadding(16,16,16,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,12)}
            }

            card.addView(TextView(this).apply { text=name; textSize=17f; setTypeface(null,Typeface.BOLD) })
            if(address.isNotEmpty()) card.addView(TextView(this).apply { text="📍 $address"; textSize=12f; setTextColor(Color.GRAY) })
            if(phone.isNotEmpty()) card.addView(TextView(this).apply { text="📞 $phone"; textSize=12f; setTextColor(Color.GRAY) })
            if(tin.isNotEmpty()) card.addView(TextView(this).apply { text="TIN: $tin"; textSize=11f; setTextColor(Color.GRAY) })
            card.addView(TextView(this).apply { text="Footer: $footer"; textSize=11f; setTextColor(Color.parseColor("#1D4ED8")); setPadding(0,6,0,10) })

            val rowBtn = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val btnEdit = Button(this).apply { text="EDIT"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,6,0)} }
            btnEdit.setOnClickListener { showAddEditDialog(shopId, raw.toString()) }
            val btnDel = Button(this).apply { text="DELETE"; setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            btnDel.setOnClickListener {
                AlertDialog.Builder(this@ManageShopsActivity).setTitle("Delete $name?").setPositiveButton("Delete"){_,_->
                    pref.edit().remove(shopId).apply()
                    Toast.makeText(this@ManageShopsActivity,"Deleted",Toast.LENGTH_SHORT).show()
                    refreshShops()
                }.setNegativeButton("Cancel",null).show()
            }
            rowBtn.addView(btnEdit); rowBtn.addView(btnDel)
            card.addView(rowBtn)
            shopContainer.addView(card)
        }
    }

    private fun showAddEditDialog(shopId: String?, oldRaw: String?){
        val parts = oldRaw?.split("|")
        val isEdit = shopId!=null

        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(30,20,30,20); setBackgroundColor(Color.WHITE) }
        lay.addView(TextView(this).apply { text=if(isEdit) "Edit Shop - Receipt Editable" else "Add New Shop - Receipt Editable"; textSize=18f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,0,0,12) })

        fun makeInput(hint:String, def:String): EditText {
            return EditText(this).apply {
                this.hint=hint; setText(def); setPadding(20,16,20,16); setBackgroundColor(Color.parseColor("#F1F5F9"))
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)}
            }
        }

        val inputName = makeInput("Shop Name * e.g. Main Shop - HEADER", parts?.getOrNull(0)?:"")
        val inputAddr = makeInput("Address e.g. Corner Samora & 1st, Harare - Line 2", parts?.getOrNull(1)?:"")
        val inputPhone = makeInput("Phone e.g. +263 77 123 4567 - Tel:", parts?.getOrNull(2)?:"")
        val inputTin = makeInput("TIN / VAT No. e.g. TIN: 12345", parts?.getOrNull(3)?:"")
        val inputFooter = makeInput("Receipt Footer e.g. Thank you! Come again!", parts?.getOrNull(4)?:"Thank you for shopping!")

        lay.addView(inputName); lay.addView(inputAddr); lay.addView(inputPhone); lay.addView(inputTin); lay.addView(inputFooter)

        val btnSave = Button(this).apply { text=if(isEdit) "UPDATE SHOP" else "SAVE SHOP"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
        lay.addView(btnSave)

        val dialog = AlertDialog.Builder(this).setView(lay).create()

        btnSave.setOnClickListener {
            val name = inputName.text.toString().trim()
            if(name.isEmpty()){ Toast.makeText(this,"Enter shop name",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            // Double check branch limit on save (for ADD only)
            if(!isEdit){
                val currentCount = getSharedPreferences("shops_db", Context.MODE_PRIVATE).all.size
                if(currentCount >= LicenseManager.getMaxBranches(this)){
                    Toast.makeText(this,"Limit reached! Activate more branches",Toast.LENGTH_LONG).show(); return@setOnClickListener
                }
            }

            val address = inputAddr.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val tin = inputTin.text.toString().trim()
            val footer = inputFooter.text.toString().trim().ifEmpty { "Thank you!" }

            val id = shopId?: "shop_${System.currentTimeMillis()}"
            val value = "$name|$address|$phone|$tin|$footer|$id"

            getSharedPreferences("shops_db", Context.MODE_PRIVATE).edit().putString(id, value).apply()

            Toast.makeText(this,"✅ Saved: $name - Receipt updated!",Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshShops()
        }
        dialog.show()
    }
}
