package com.smartpos

import android.content.Context
import android.content.SharedPreferences

object LicenseManager {

    private const val PREFS_NAME = "license_prefs"

    private fun getPrefs(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLicenseValid(ctx: Context): Boolean {
        val expiry = getPrefs(ctx).getLong("expiry_time", 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getMaxBranches(ctx: Context): Int {
        return getPrefs(ctx).getInt("max_branches", 1)
    }

    fun getStatusText(ctx: Context): String {
        val prefs = getPrefs(ctx)
        val expiry = prefs.getLong("expiry_time", 0L)
        val max = prefs.getInt("max_branches", 1)
        val shopsCount = ctx.getSharedPreferences("shops_db", Context.MODE_PRIVATE).all.size
        if (expiry == 0L) return "NO LICENSE"
        if (expiry < System.currentTimeMillis()) return "EXPIRED $shopsCount/$max"
        val daysLeft = (expiry - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        return "VALID ${daysLeft + 1} days | $shopsCount/$max shops"
    }

    // SIMPLE OFFLINE ACTIVATION - NO INTERNET, NO JSON - WILL ALWAYS BUILD
    fun verifyLicense(ctx: Context, code: String, callback: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        var days = 0
        var branches = 3

        when (cleanCode) {
            "TEST-14" -> { days = 14; branches = 3 }
            "TEST-30" -> { days = 30; branches = 3 }
            "SMART-14" -> { days = 14; branches = 3 }
            "SMART-30" -> { days = 30; branches = 3 }
            "SMART-90" -> { days = 90; branches = 5 }
            "SMART-365" -> { days = 365; branches = 10 }
            "CHT-3-30-XXX" -> { days = 30; branches = 3 }
        }

        if (days > 0) {
            val newExpiry = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
            getPrefs(ctx).edit()
                .putLong("expiry_time", newExpiry)
                .putInt("max_branches", branches)
                .putString("license_code", cleanCode)
                .apply()
            callback(true, "Activated $days days | $branches branches")
        } else {
            callback(false, "Invalid code: $cleanCode")
        }
    }
}