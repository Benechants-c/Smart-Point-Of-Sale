package com.smartpos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams

class ReceiveStockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER
        }
        
        val title = TextView(this).apply {
            text = "Receive Stock"
            textSize = 22f
            setPadding(0, 0, 0, 30)
        }
        
        val nameInput = EditText(this).apply { hint = "Product Name" }
        val qtyInput = EditText(this).apply { hint = "Quantity"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val costInput = EditText(this).apply { hint = "Buying Price"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        
        val saveBtn = Button(this).apply { text = "Save Stock" }
        
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 0) }
        
        layout.addView(title)
        layout.addView(nameInput, params)
        layout.addView(qtyInput, params)
        layout.addView(costInput, params)
        layout.addView(saveBtn, params)
        
        saveBtn.setOnClickListener {
            Toast.makeText(this, "Saved: ${nameInput.text} x ${qtyInput.text}", Toast.LENGTH_SHORT).show()
            nameInput.text.clear()
            qtyInput.text.clear()
            costInput.text.clear()
        }
        
        setContentView(layout)
    }
}
