package com.smartpos

import android.content.Context
import java.util.Calendar
import java.util.concurrent.TimeUnit

object LicenseManager {
    fun getDaysInCurrentMonth(): Int {
        return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    fun getExpiryDate(c: Context): Long {
        return c.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry_date", 0L)
    }
    fun setNewSubscription(c: Context) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, getDaysInCurrentMonth())
        c.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
           .putLong("expiry_date", cal.timeInMillis).putBoolean("is_active", true).apply()
    }
    fun daysLeft(c: Context): Int {
        val exp = getExpiryDate(c)
        if(exp==0L) return 30
        return TimeUnit.MILLISECONDS.toDays(exp - System.currentTimeMillis()).toInt()
    }
    fun isExpired(c: Context): Boolean { return daysLeft(c) <= 0 }
    fun shouldShowWarning(c: Context): Boolean { return daysLeft(c) in 1..5 }
    fun reactivateClient(c: Context, maxBranches: Int) {
        setNewSubscription(c)
        c.getSharedPreferences("shops_config", Context.MODE_PRIVATE).edit().putInt("max_branches", maxBranches).apply()
    }
}
