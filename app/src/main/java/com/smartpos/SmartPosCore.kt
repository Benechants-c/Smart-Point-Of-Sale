package com.smartpos

// SMART POS COMMERCIAL EDITION - $12/month
// Offline + Online + BusinessID + Audit Trail

data class Business(
    val businessId: String, // e.g., SHOP001-HRE
    val shopName: String,
    val ownerPhone: String,
    val licenseExpiry: Long
)

enum class UserRole { ADMIN, MANAGER, CASHIER }

data class PosUser(
    val userId: String,
    val name: String,
    val role: UserRole,
    val businessId: String,
    val pin: String
)

// AUDIT TRAIL - ZIMRA COMPLIANT - Who sold what, when
data class AuditLog(
    val id: String = System.currentTimeMillis().toString(),
    val businessId: String,
    val userId: String,
    val userName: String,
    val action: String, // SALE, STOCK_ADD, STOCK_RECEIVE, VOID, LOGIN
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

// CATEGORIES - Shop can create own
data class Category(
    val id: String,
    val businessId: String,
    val name: String // e.g., Groceries, Drinks, Airtime
)

// STOCK - With Receive Goods
data class Product(
    val id: String,
    val businessId: String,
    val name: String,
    val categoryId: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stockQty: Int,
    val minStockAlert: Int = 5
)

data class Sale(
    val id: String,
    val businessId: String,
    val cashierId: String,
    val items: List<SaleItem>,
    val total: Double,
    val timestamp: Long
)

data class SaleItem(
    val productId: String,
    val qty: Int,
    val price: Double
)

// License Check
object LicenseManager {
    fun isLicenseValid(business: Business): Boolean {
        return System.currentTimeMillis() < business.licenseExpiry
    }
}
