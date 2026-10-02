package com.smartpos

import android.content.Context
import java.util.Calendar
import java.util.concurrent.TimeUnit

object LicenseManager {

    fun getDaysInCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH) // 30 or 31 auto!
    }

    fun getExpiryDate(context: Context): Long {
        val pref = context.getSharedPreferences("license", Context.MODE_PRIVATE)
        return pref.getLong("expiry_date", 0L)
    }

    fun setNewSubscription(context: Context) {
        val cal = Calendar.getInstance()
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH) // 30 or 31
        cal.add(Calendar.DAY_OF_MONTH, days)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val expiry = cal.timeInMillis
        context.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
           .putLong("expiry_date", expiry)
           .putLong("start_date", System.currentTimeMillis())
           .putBoolean("is_active", true)
           .apply()
    }

    fun daysLeft(context: Context): Int {
        val expiry = getExpiryDate(context)
        if(expiry==0L) return 30
        val diff = expiry - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    fun isExpired(context: Context): Boolean {
        return daysLeft(context) <= 0
    }

    fun shouldShowWarning(context: Context): Boolean {
        val left = daysLeft(context)
        return left in 1..5 // Warn within 5 days!
    }

    fun isActive(context: Context): Boolean {
        val pref = context.getSharedPreferences("license", Context.MODE_PRIVATE)
        return pref.getBoolean("is_active", true) &&!isExpired(context)
    }

    fun reactivateClient(context: Context, maxBranches:Int) {
        setNewSubscription(context)
        context.getSharedPreferences("shops_config", Context.MODE_PRIVATE).edit()
           .putInt("max_branches", maxBranches)
           .apply()
    }
}
