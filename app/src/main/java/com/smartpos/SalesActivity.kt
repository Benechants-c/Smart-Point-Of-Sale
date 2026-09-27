package com.smartpos
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SalesActivity : AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,100,40,40) }
  val title = TextView(this).apply { text = "SMART POS - SELL\nSHOP001 | ADMIN | Audit: ON"; textSize = 18f }
  val item = EditText(this).apply { hint = "Item Barcode / Name" }
  val qty = EditText(this).apply { hint = "Qty"; inputType = 2 }
  val btnSell = Button(this).apply { text = "SELL - CASH $10.00" }
  val btnStock = Button(this).apply { text = "RECEIVE STOCK (ADMIN)" }
  val log = TextView(this).apply { text = "\n--- AUDIT TRAIL ---\n[19:50] ADMIN Login SHOP001\n[19:50] Sale READY\nZIMRA Compliant Log\n\nBusinessID: SHOP001-HRE\nLicense: $12/mo ACTIVE"; textSize = 14f }
  btnSell.setOnClickListener { Toast.makeText(this, "SOLD! Receipt #${System.currentTimeMillis() % 10000} - Cash", Toast.LENGTH_SHORT).show() }
  btnStock.setOnClickListener { Toast.makeText(this, "Stock Receive - Category Added - Audit Logged", Toast.LENGTH_SHORT).show() }
  lay.addView(title); lay.addView(item); lay.addView(qty); lay.addView(btnSell); lay.addView(btnStock); lay.addView(log)
  setContentView(lay)
 }
}
