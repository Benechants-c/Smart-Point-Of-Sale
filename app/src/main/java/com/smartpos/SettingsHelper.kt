package com.smartpos

import android.content.Context

object SettingsHelper {
    fun getShopName(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE)
            .getString("shop_name","Main Shop")!!
    }
    fun getCurrency(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE)
            .getString("currency","$")!!
    }
    // TAX INCLUSIVE - ALWAYS 0
    fun isTaxInclusive(): Boolean = true

    fun getAddress(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE)
            .getString("address","Harare")!!
    }
    fun getPhone(ctx: Context): String {
        return ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE)
            .getString("phone","")!!
    }
    fun getReceiptHeader(ctx: Context): String {
        return ctx.getSharedPreferences("receipt_settings", Context.MODE_PRIVATE)
            .getString("header","*** THANK YOU ***")!!
    }
    fun getReceiptFooter(ctx: Context): String {
        return ctx.getSharedPreferences("receipt_settings", Context.MODE_PRIVATE)
            .getString("footer","Come Again!")!!
    }
    fun showShop(ctx: Context): Boolean {
        return ctx.getSharedPreferences("receipt_settings",0).getBoolean("show_shop",true)
    }
    fun showAddress(ctx: Context): Boolean {
        return ctx.getSharedPreferences("receipt_settings",0).getBoolean("show_address",true)
    }
    fun showPhone(ctx: Context): Boolean {
        return ctx.getSharedPreferences("receipt_settings",0).getBoolean("show_phone",true)
    }
    fun showDate(ctx: Context): Boolean {
        return ctx.getSharedPreferences("receipt_settings",0).getBoolean("show_date",true)
    }
}