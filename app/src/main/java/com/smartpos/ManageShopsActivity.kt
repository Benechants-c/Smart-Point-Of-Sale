package com.smartpos

import android.app.Activity
import android.app.AlertDialog
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
            text="🏪 Manage Shops - Receipt Details"; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,12)
        })

        val btnAdd = Button(this).apply {
            text="➕ ADD NEW SHOP"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setPadding(0,20,0,20); textSize=16f
            setOnClickListener { showAddEditDialog(null, null) }
        }
        root.addView(btnAdd)

        shopContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,12,0,0) }
        val scroll = ScrollView(this).apply { addView(shopContainer) }
        root.addView(scroll)

        setContentView(root)
        refreshShops()
    }

    private fun refreshShops(){
        shopContainer.removeAllViews()
        val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE)

        if(pref.all.isEmpty()){
            shopContainer.addView(TextView(this).apply { text="No shops yet. Add one."; gravity=Gravity.CENTER; setPadding(0,40,0,0) })
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

        lay.addView(TextView(this).apply { text=if(isEdit) "Edit Shop" else "Add New Shop"; textSize=18f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,0,0,12) })

        fun makeInput(hint:String, def:String): EditText {
            return EditText(this).apply {
                this.hint=hint; setText(def); setPadding(20,16,20,16); setBackgroundColor(Color.parseColor("#F1F5F9"))
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)}
            }
        }

        val inputName = makeInput("Shop Name * e.g. Main Shop", parts?.getOrNull(0)?:"")
        val inputAddr = makeInput("Address e.g. Corner Samora & 1st, Harare", parts?.getOrNull(1)?:"")
        val inputPhone = makeInput("Phone e.g. +263 77 123 4567", parts?.getOrNull(2)?:"")
        val inputTin = makeInput("TIN / VAT No. e.g. TIN: 12345", parts?.getOrNull(3)?:"")
        val inputFooter = makeInput("Receipt Footer e.g. Thank you!", parts?.getOrNull(4)?:"Thank you for shopping!")

        lay.addView(inputName); lay.addView(inputAddr); lay.addView(inputPhone); lay.addView(inputTin); lay.addView(inputFooter)

        val btnSave = Button(this).apply { text=if(isEdit) "UPDATE SHOP" else "SAVE SHOP"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }

        lay.addView(btnSave)

        val dialog = AlertDialog.Builder(this).setView(lay).create()

        btnSave.setOnClickListener {
            val name = inputName.text.toString().trim()
            if(name.isEmpty()){ Toast.makeText(this,"Enter shop name",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val address = inputAddr.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val tin = inputTin.text.toString().trim()
            val footer = inputFooter.text.toString().trim().ifEmpty { "Thank you!" }

            val id = shopId?: "shop_${System.currentTimeMillis()}"
            val value = "$name|$address|$phone|$tin|$footer"

            getSharedPreferences("shops_db", Context.MODE_PRIVATE).edit().putString(id, value).apply()

            Toast.makeText(this,"Saved: $name",Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshShops()
        }

        dialog.show()
    }
}
