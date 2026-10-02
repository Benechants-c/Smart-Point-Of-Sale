package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class UsersActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30,40,30,30)
        }
        topBar.addView(TextView(this).apply { text=" <- Back"; setTextColor(Color.WHITE); setOnClickListener{ finish() } })
        topBar.addView(TextView(this).apply { text=" Create Users"; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); setPadding(20,0,0,0) })
        root.addView(topBar)

        // Form
        val form = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20); setBackgroundColor(Color.WHITE) }

        val nameBox = EditText(this).apply { hint="Cashier Name e.g John" }
        val pinBox = EditText(this).apply { hint="PIN 4-6 digits OR Password (e.g 1234 or john123)" }

        val roleSpinner = Spinner(this)
        roleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Manager","Cashier")).apply{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val shops = arrayOf("Main Shop","Branch 1","Branch 2")
        val shopSpinner = Spinner(this)
        shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shops).apply{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Permissions - Admin ticks what Manager/Cashier can do
        val cbSell = CheckBox(this).apply { text="Can SELL"; isChecked=true }
        val cbReport = CheckBox(this).apply { text="Can View REPORTS" }
        val cbStock = CheckBox(this).apply { text="Can Manage STOCK" }

        val btnAdd = Button(this).apply {
            text="Add User"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
        }

        val listContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(10,10,10,10) }
        val scroll = ScrollView(this).apply { addView(listContainer) }

        btnAdd.setOnClickListener{
            val name = nameBox.text.toString().trim()
            val pin = pinBox.text.toString().trim()

            // CHECK: PIN must be 4-6 characters
            if(name.isEmpty()){
                Toast.makeText(this,"Enter name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(pin.length < 4 || pin.length > 6){
                Toast.makeText(this,"PIN/Password must be 4-6 chars",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = roleSpinner.selectedItem.toString()
            val shop = shopSpinner.selectedItem.toString()

            // Save user: format = name|pin|role|shop|canSell|canReport|canStock
            val value = "$name|$pin|$role|$shop|${cbSell.isChecked}|${cbReport.isChecked}|${cbStock.isChecked}"
            getSharedPreferences("users_db", Context.MODE_PRIVATE).edit()
               .putString("user_$name", value).apply()

            Toast.makeText(this,"$role $name added! PIN: $pin",Toast.LENGTH_SHORT).show()
            nameBox.text.clear(); pinBox.text.clear()

            // Refresh list
            listContainer.removeAllViews()
            val pref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
            for((k,v) in pref.all){
                if(!k.startsWith("user_")) continue
                val parts = v.toString().split("|")
                val tv = TextView(this).apply {
                    text="${parts[0]} - ${parts[2]} - PIN:${parts[1]} - ${parts[3]}"
                    setPadding(10,10,10,10)
                    setBackgroundColor(Color.parseColor("#E2E8F0"))
                    setMargins(0,0,0,10)
                }
                listContainer.addView(tv)
            }
        }

        form.addView(nameBox); form.addView(pinBox); form.addView(roleSpinner); form.addView(shopSpinner)
        form.addView(cbSell); form.addView(cbReport); form.addView(cbStock); form.addView(btnAdd)

        root.addView(form)
        root.addView(scroll)

        setContentView(root)

        // Load existing users on open
        val pref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
        for((k,v) in pref.all){
            if(!k.startsWith("user_")) continue
            val parts = v.toString().split("|")
            val tv = TextView(this).apply {
                text="${parts[0]} - ${parts[2]} - PIN:${parts[1]}"
                setPadding(10,10,10,10)
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                setMargins(0,0,0,10)
            }
            listContainer.addView(tv)
        }
    }

    fun android.view.View.setMargins(l:Int,t:Int,r:Int,b:Int){
        val p = layoutParams as? LinearLayout.LayoutParams?: LinearLayout.LayoutParams(-1,-2)
        p.setMargins(l,t,r,b); layoutParams=p
    }
}
