package com.smartpos

import android.content.Context

object SettingsHelper {
    fun getCurrency(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE).getString("currency","$")?: "$"
    }
    fun getShopName(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE).getString("shop_name","Main Shop")?: "Main Shop"
    }
    fun getShopAddress(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE).getString("address","")?: ""
    }
    fun getShopPhone(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE).getString("phone","")?: ""
    }
    fun getReceiptHeader(ctx: Context): String {
        return ctx.getSharedPreferences("receipt_settings", Context.MODE_PRIVATE).getString("header","*** THANK YOU ***")?: "*** THANK YOU ***"
    }
    fun getReceiptFooter(ctx: Context): String {
        return ctx.getSharedPreferences("receipt_settings", Context.MODE_PRIVATE).getString("footer","Come Again!")?: "Come Again!"
    }
}