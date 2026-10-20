package com.smartpos

data class StockItem(val code: String, val name: String, val qty: Int, val cost: Double, val selling: Double)
data class SaleRecord(val date: String, val code: String, val product: String, val qty: Int, val cost: Double, val selling: Double)
data class CustomerItem(val id: String, val name: String, val phone: String, val location: String, val createdDate: String)
data class TakeEntry(val stockItem: StockItem, var counted: Int? = null, var variance: Int? = null)
data class PriceEntry(val stockItem: StockItem, var newPrice: Double? = null)
data class AuditEntry(val account: String, val dateTime: String, val action: String, val details: String, val type: String)