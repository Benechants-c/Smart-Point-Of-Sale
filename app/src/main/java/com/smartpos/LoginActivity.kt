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
data class StockItem(val name: String, val cost: Double, val price: Double, var qty: Int)
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
        "1002" to StockItem("Milk 500ml", 0.8, 1.0, 50),
        "1005" to StockItem("Bread", 0.5, 0.8, 30),
        "1010" to StockItem("Sugar 1kg", 1.2, 1.5, 20),
        "1020" to StockItem("Coke 330ml", 0.4, 0.6, 100)
    ))}
    var cart by remember { mutableStateOf(mutableListOf<CartItem>()) }
    var receivingHistory by remember { mutableStateOf(mutableListOf<ReceivingRecord>()) }

    if (!isLoggedIn) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("SmartShop POS", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN 0000 / 1234") })
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (pin == "0000") { role = "cashier"; isLoggedIn = true; screen = "sales" }
                else if (pin == "1234") { role = "admin"; isLoggedIn = true; screen = "admin_home" }
            }, Modifier.fillMaxWidth()) { Text("LOGIN") }
        }
    } else {
        when (screen) {
            "sales" -> SalesScreen(stockMap, cart, { cart = it }, { isLoggedIn = false; pin = "" }, role, { screen = "admin_home" })
            "admin_home" -> AdminHomeScreen({ screen = it }, { isLoggedIn = false; pin = "" })
            "receiving" -> ReceivingScreen(stockMap, { stockMap = it }, receivingHistory, { receivingHistory = it }, { screen = "admin_home" })
            else -> AdminHomeScreen({ screen = it }, { isLoggedIn = false; pin = "" })
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
        Button(onClick = { onNavigate("sales") }, Modifier.fillMaxWidth().height(60.dp)) { Text("SALES") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onNavigate("receiving") }, Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("RECEIVING") }
    }
}

@Composable
fun SalesScreen(stockMap: MutableMap<String, StockItem>, cart: MutableList<CartItem>, onCartChange: (MutableList<CartItem>) -> Unit, onLogout: () -> Unit, role: String, onAdminBack: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var receivedText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val filtered = stockMap.filter { it.key.contains(search, true) || it.value.name.contains(search, true) }.toList()
    val total = cart.sumOf { it.price * it.qty }
    val received = receivedText.toDoubleOrNull()?: 0.0
    val change = received - total
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if(role=="admin") "SALES ADMIN" else "SALES", fontWeight = FontWeight.Bold)
            Row {
                if(role=="admin") Button(onClick = onAdminBack, modifier = Modifier.padding(end=6.dp)) { Text("BACK") }
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search OUR CODE e.g. 1002 or milk") }, modifier = Modifier.fillMaxWidth())
        if (search.isNotEmpty()) {
            LazyColumn(Modifier.height(120.dp)) {
                items(filtered) { (code, item) ->
                    Button(onClick = {
                        val ex = cart.find { it.code == code }
                        if (ex != null) ex.qty++ else cart.add(CartItem(code, item.name, item.price, 1))
                        onCartChange(cart.toMutableList()); search = ""
                    }, Modifier.fillMaxWidth().padding(2.dp)) { Text("$code - ${item.name} - ${item.price} - Stock ${item.qty}") }
                }
            }
        }
        Text("Cart: ${if(cart.isEmpty()) "Cart empty - Search product" else ""}", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(cart.toList()) { item ->
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.code} ${item.name} x${item.qty}")
                    Row {
                        Button(onClick = { val c = cart.toMutableList(); val f = c.find { it.code == item.code }; if(f!=null){ if(f.qty>1) f.qty-- else c.remove(f); onCartChange(c) } }, modifier=Modifier.width(40.dp)) { Text("-") }
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
            Text("Change: $${String.format("%.2f", if(change>0) change else 0.0)}", fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { if(total>0 && received >= total){ message = "Sold Change ${String.format("%.2f", change)}"; onCartChange(mutableListOf()); receivedText = "" } }, Modifier.weight(1f)) { Text("CASH") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if(total>0){ message = "Sold ECOCASH"; onCartChange(mutableListOf()) } }, Modifier.weight(1f)) { Text("ECOCASH") }
        }
        if(message.isNotEmpty()) Text(message, color = Color.Green)
    }
}

@Composable
fun ReceivingScreen(stockMap: MutableMap<String, StockItem>, onStockUpdate: (MutableMap<String, StockItem>) -> Unit, history: MutableList<ReceivingRecord>, onHistoryUpdate: (MutableList<ReceivingRecord>) -> Unit, onBack: () -> Unit) {
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
    val filtered = stockMap.filter { it.key.contains(searchCode, true) || it.value.name.contains(searchCode, true) }.toList()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RECEIVING", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Button(onClick = onBack) { Text("BACK") }
        }
        OutlinedTextField(value = searchCode, onValueChange = { searchCode = it; if(it.isEmpty()){ selectedCode=""; selectedName="" } }, label = { Text("Search OUR SHOP CODE or name - e.g. 1002") }, modifier = Modifier.fillMaxWidth())
        if (searchCode.isNotEmpty() && selectedCode.isEmpty()) {
            LazyColumn(Modifier.height(100.dp)) {
                items(filtered) { (code, item) ->
                    Button(onClick = { selectedCode = code; selectedName = item.name; currentStock = item.qty; cost = item.cost.toString(); price = item.price.toString(); searchCode = "$code - ${item.name}" }, Modifier.fillMaxWidth().padding(2.dp)) { Text("$code - ${item.name} Stock:${item.qty}") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if(selectedCode.isNotEmpty() || searchCode.length>1){
            if(selectedCode.isEmpty()){
                OutlinedTextField(value = selectedCode, onValueChange = { selectedCode = it }, label = { Text("New Shop Code* e.g. 1099") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = selectedName, onValueChange = { selectedName = it }, label = { Text("New Product Name*") }, modifier = Modifier.fillMaxWidth())
            } else {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Shop Code: $selectedCode", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text("Name: $selectedName - Stock: $currentStock")
                        Text("Auto-picked Cost: $cost Price: $price - change only if needed", fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty*") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier opt") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = invoice, onValueChange = { invoice = it }, label = { Text("Invoice No - NOT mandatory (black market)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val q = qty.toIntOrNull()?: 0
                if(selectedCode.isEmpty() || q<=0){ msg = "Enter Code + Qty"; return@Button }
                val c = cost.toDoubleOrNull()?: stockMap[selectedCode]?.cost?: 0.0
                val p = price.toDoubleOrNull()?: stockMap[selectedCode]?.price?: 0.0
                val old = stockMap[selectedCode]
                val newQty = (old?.qty?: 0) + q
                val nameSave = if(selectedName.isNotEmpty()) selectedName else old?.name?: "New Item"
                val newMap = stockMap.toMutableMap()
                newMap[selectedCode] = StockItem(nameSave, c, p, newQty)
                onStockUpdate(newMap)
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val rec = ReceivingRecord(sdf.format(Date()), selectedCode, nameSave, q, c, p, if(supplier.isEmpty()) "No Supplier" else supplier)
                val nh = history.toMutableList(); nh.add(0, rec); onHistoryUpdate(nh)
                msg = "Received $q x $nameSave - Now $newQty"
                selectedCode=""; selectedName=""; qty=""; searchCode=""; currentStock=0
            }, Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("RECEIVE STOCK") }
            if(msg.isNotEmpty()) Text(msg, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Today Received:", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(history.toList()) { r ->
                Card(Modifier.fillMaxWidth().padding(2.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        Text("${r.date} | ${r.code} ${r.name} x${r.qty}", fontSize = 12.sp)
                        Text("Cost ${r.cost} Price ${r.price} Supplier ${r.supplier}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
