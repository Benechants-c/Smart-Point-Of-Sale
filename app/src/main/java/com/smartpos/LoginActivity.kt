package com.smartpos
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class LoginActivity : AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60,200,60,60) }
  val title = TextView(this).apply { text = "SMART POS LOGIN\n$12/mo Commercial"; textSize = 20f }
  val biz = EditText(this).apply { hint = "BusinessID: SHOP001-HRE" }
  val pin = EditText(this).apply { hint = "PIN: 1234 ADMIN"; inputType = 129 }
  val btn = Button(this).apply { text = "LOGIN" }
  btn.setOnClickListener { Toast.makeText(this, "ADMIN Login - Audit ON", Toast.LENGTH_SHORT).show() }
  lay.addView(title); lay.addView(biz); lay.addView(pin); lay.addView(btn)
  setContentView(lay)
 }
}
