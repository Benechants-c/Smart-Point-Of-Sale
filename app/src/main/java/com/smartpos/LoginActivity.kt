package com.smartshop.posv5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(val code: String, val name: String, val price: Double, var qty: Int)
data class ReceivingRecord(val date: String, val code: String, val name: String, val qty: Int, val cost: Double, val price: Double, val supplier: String)

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosApp() }
    }
}

@Composable
fun PosApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("cashier") }
    var pin by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf("login") }

    var stockMap by remember { mutableStateOf(mutableMapOf(
        "1002" to Triple("Milk 500ml", Pair(0.8, 1.0), 50),
        "1005" to Triple("Bread", Pair(0.5, 0.8), 30),
        "1010" to Triple("Sugar 1kg", Pair(1.2, 1.5), 20),
        "1020" to Triple("Coke 330ml", Pair(0.4, 0.6), 100)
    ))}
    var cart by remember { mutableStateOf(mutableListOf<CartItem>()) }
    var receivingHistory by remember { mutableStateOf(mutableListOf<ReceivingRecord>()) }

    if (!isLoggedIn) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("SmartShop POS", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN 0000 Cashier / 1234 Admin") })
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (pin == "0000") { role = "cashier"; isLoggedIn = true; screen = "sales" }
                else if (pin == "1234") { role = "admin"; isLoggedIn = true; screen = "admin_home" }
            }, Modifier.fillMaxWidth()) { Text("LOGIN") }
        }
    } else {
        when (screen) {
            "sales" -> SalesScreen(stockMap, cart, onCartChange = { cart = it }, onLogout = { isLoggedIn = false; pin = "" }, role = role, onAdminBack = { screen = "admin_home" })
            "admin_home" -> AdminHomeScreen(onNavigate = { screen = it }, onLogout = { isLoggedIn = false; pin = "" })
            "receiving" -> ReceivingScreen(stockMap = stockMap, onStockUpdate = { stockMap = it }, history = receivingHistory, onHistoryUpdate = { receivingHistory = it }, onBack = { screen = "admin_home" })
            else -> AdminHomeScreen(onNavigate = { screen = it }, onLogout = { isLoggedIn = false; pin = "" })
        }
    }
}

@Composable
fun AdminHomeScreen(onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ADMIN HOME", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("LOGOUT") }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onNavigate("sales") }, Modifier.fillMaxWidth().height(60.dp)) { Text("SALES (POS)", fontSize = 18.sp) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onNavigate("receiving") }, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("RECEIVING", fontSize = 18.sp) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {}, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("STOCK (Coming Next)") }
    }
}

@Composable
fun SalesScreen(stockMap: MutableMap<String, Triple<String, Pair<Double, Double>, Int>>, cart: MutableList<CartItem>, onCartChange: (MutableList<CartItem>) -> Unit, onLogout: () -> Unit, role: String, onAdminBack: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var receivedText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val filtered = stockMap.filter { it.key.contains(search, true) || it.value.first.contains(search, true) }.toList()
    val total = cart.sumOf { it.price * it.qty }
    val received = receivedText.toDoubleOrNull()?: 0.0
    val change = received - total
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if(role=="admin") "SALES - ADMIN" else "SALES - Cashier", fontWeight = FontWeight.Bold)
            Row {
                if(role=="admin") { Button(onClick = onAdminBack, modifier = Modifier.padding(end=6.dp)) { Text("BACK") } }
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search code / name e.g. 1002 milk") }, modifier = Modifier.fillMaxWidth())
        if (search.isNotEmpty()) {
            LazyColumn(Modifier.height(120.dp)) {
                items(filtered) { (code, data) ->
                    val price = data.second.second
                    Button(onClick = {
                        val existing = cart.find { it.code == code }
                        if (existing!= null) existing.qty++ else cart.add(CartItem(code, data.first, price, 1))
                        onCartChange(cart.toMutableList()); search = ""
                    }, Modifier.fillMaxWidth().padding(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                        Text("$code - ${data.first} - $price - Stock ${data.third}")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Cart: ${if(cart.isEmpty()) "Cart empty - Search product" else ""}", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(cart.toList()) { item ->
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.code} ${item.name} x${item.qty} = ${String.format("%.2f", item.price*item.qty)}")
                    Row {
                        Button(onClick = { val c = cart.toMutableList(); val f = c.find { it.code == item.code }; if(f!=null){ if(f.qty>1) f.qty-- else c.remove(f); onCartChange(c) } }, modifier=Modifier.width(40.dp)) { Text("-") }
                        Text(" ${item.qty} ", modifier = Modifier.padding(horizontal=4.dp))
                        Button(onClick = { val c = cart.toMutableList(); c.find { it.code == item.code }?.let{ it.qty++ }; onCartChange(c) }, modifier=Modifier.width(40.dp)) { Text("+") }
                    }
                }
            }
        }
        HorizontalDivider()
        Text("TOTAL: $${String.format("%.2f", total)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(value = receivedText, onValueChange = { receivedText = it }, label = { Text("Received") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text("Change: $${String.format("%.2f", if(change>0) change else 0.0)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { if(total>0){ if(received >= total || role=="admin"){ message = "Sold Change ${String.format("%.2f", change)}"; onCartChange(mutableListOf()); receivedText = "" } else message = "Insufficient cash" } }, Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("CASH") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if(total>0){ message = "Sold ECOCASH"; onCartChange(mutableListOf()); receivedText = "" } }, Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) { Text("ECOCASH") }
        }
        if(message.isNotEmpty()) Text(message, color = Color.Green, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReceivingScreen(stockMap: MutableMap<String, Triple<String, Pair<Double, Double>, Int>>, onStockUpdate: (MutableMap<String, Triple<String, Pair<Double, Double>, Int>>) -> Unit, history: MutableList<ReceivingRecord>, onHistoryUpdate: (MutableList<ReceivingRecord>) -> Unit, onBack: () -> Unit) {
    var searchCode by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }
    var currentStock by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var invoice by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val filtered = stockMap.filter { it.key.contains(searchCode, true) || it.value.first.contains(searchCode, true) }.toList()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RECEIVING", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Button(onClick = onBack) { Text("BACK") }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = searchCode, onValueChange = { searchCode = it }, label = { Text("Search OUR SHOP CODE or name") }, modifier = Modifier.fillMaxWidth())
        if (searchCode.isNotEmpty() && selectedCode.isEmpty()) {
            LazyColumn(Modifier.height(100.dp)) {
                items(filtered) { (code, data) ->
                    Button(onClick = { selectedCode = code; selectedName = data.first; currentStock = data.third; cost = data.second.first.toString(); price = data.second.second.toString(); searchCode = "$code - ${data.first}" }, Modifier.fillMaxWidth().padding(2.dp)) { Text("$code - ${data.first} - Stock:${data.third}") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if(searchCode.isNotEmpty() || selectedCode.isNotEmpty()){
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(10.dp)) {
                    Text("Shop Code: $selectedCode", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Name: $selectedName - Current Stock: $currentStock")
                    Text("Old Cost: $cost | Old Price: $price - will auto-pick if already in stock")
                }
            }
            Spacer(Modifier.height(10.dp))
            if(selectedCode.isEmpty().not() && stockMap.containsKey(selectedCode).not()){
                // new product manual entry handled by searchCode
            }
            if(!stockMap.containsKey(selectedCode) && selectedCode.isNotEmpty()){
                OutlinedTextField(value = selectedName, onValueChange = { selectedName = it }, label = { Text("New Product Name*") }, modifier = Modifier.fillMaxWidth())
            }
            if(selectedCode.isEmpty()){
                OutlinedTextField(value = selectedCode, onValueChange = { selectedCode = it }, label = { Text("New Shop Code* e.g. 1099") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = selectedName, onValueChange = { selectedName = it }, label = { Text("New Product Name*") }, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty Received*") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost (auto)") }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (auto)") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier optional") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = invoice, onValueChange = { invoice = it }, label = { Text("Invoice No - NOT mandatory") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                val q = qty.toIntOrNull()?: 0
                if(selectedCode.isEmpty() || q<=0){ msg = "Enter Code + Qty"; return@Button }
                val c = cost.toDoubleOrNull()?: stockMap[selectedCode]?.second?.first?: 0.0
                val p = price.toDoubleOrNull()?: stockMap[selectedCode]?.second?.second?: 0.0
                val existing = stockMap[selectedCode]
                val newStockQty = (existing?.third?: 0) + q
                val nameToSave = if(selectedName.isNotEmpty()) selectedName else existing?.first?: "New Item"
                val newMap = stockMap.toMutableMap()
                newMap[selectedCode] = Triple(nameToSave, Pair(c, p), newStockQty)
                onStockUpdate(newMap)
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val rec = ReceivingRecord(sdf.format(Date()), selectedCode, nameToSave, q, c, p, if(supplier.isEmpty()) "No Supplier" else supplier)
                val newHist = history.toMutableList(); newHist.add(0, rec); onHistoryUpdate(newHist)
                msg = "Received $q x $nameToSave - Stock now $newStockQty"
                selectedCode = ""; selectedName = ""; qty = ""; searchCode = ""; currentStock = 0
            }, Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("RECEIVE STOCK", fontSize = 18.sp) }
            if(msg.isNotEmpty()) Text(msg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("Today Received:", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(history.toList()) { r ->
                Card(Modifier.fillMaxWidth().padding(2.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        Text("${r.date} | ${r.code} ${r.name} x${r.qty} Cost:${r.cost} Price:${r.price}", fontSize = 12.sp)
                        Text("Supplier: ${r.supplier}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
