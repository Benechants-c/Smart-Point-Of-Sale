package com.smartpos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

class ReceiveStockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceiveStockScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Receive Stock") }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity Received") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Buying Price") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    if (name.isBlank() || qty.isBlank()) {
                        Toast.makeText(context, "Fill name and qty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    loading = true
                    val data = hashMapOf(
                        "name" to name,
                        "quantity" to (qty.toIntOrNull() ?: 0),
                        "cost" to (cost.toDoubleOrNull() ?: 0.0),
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("stock_receiving").add(data)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Stock Added!", Toast.LENGTH_SHORT).show()
                            name = ""; qty = ""; cost = ""
                            loading = false
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            loading = false
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Saving..." else "Save Stock")
            }
        }
    }
}
