package com.smartpos

import android.content.Context
import java.util.concurrent.TimeUnit

object LicenseManager {
    private const val PREF = "license_prefs"
    private const val SECRET = "Chatewa2026SmartPOS"

    fun activateLicense(context: Context, code: String): Boolean {
        try {
            val clean = code.trim().toUpperCase()
            if (!clean.startsWith("CHT-")) return false
            val parts = clean.split("-")
            if (parts.size < 4) return false

            val bCode = parts[1].toIntOrNull()?: return false
            val days = parts[2].toIntOrNull()?: return false
            val hashPart = parts[3]

            val branches = when(bCode) {
                11->1; 33->3; 55->5; 101->10; 999->999
                else-> if (bCode % 11 == 0) bCode/11 else return false
            }

            val base = SECRET + "-" + branches + "-" + days
            var hash = 0
            for (c in base) { hash = hash*31 + c.toInt() }
            hash = Math.abs(hash) % 9000 + 1000
            val expectedHex = Integer.toHexString(hash).toUpperCase()

            if (hashPart!= expectedHex) return false

            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val expireTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            prefs.edit()
               .putInt("max_branches", branches)
               .putLong("expire_time", expireTime)
               .putInt("total_days", days)
               .putString("last_code", clean)
               .putLong("activated_at", System.currentTimeMillis())
               .apply()
            return true
        } catch (e: Exception) { return false }
    }

    fun getMaxBranches(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!prefs.contains("max_branches")) return 1
        if (isExpired(context)) return 0
        return prefs.getInt("max_branches", 1)
    }

    fun getDaysLeft(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!prefs.contains("expire_time")) return 7
        val expire = prefs.getLong("expire_time", 0L)
        val diff = expire - System.currentTimeMillis()
        return if (diff <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }

    fun isExpired(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!prefs.contains("expire_time")) return false
        return System.currentTimeMillis() > prefs.getLong("expire_time", 0L)
    }

    fun canUseApp(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (prefs.contains("expire_time")) {
            return!isExpired(context)
        }
        val installPref = context.getSharedPreferences("app_first_run", Context.MODE_PRIVATE)
        var firstRun = installPref.getLong("first_run", 0L)
        if (firstRun == 0L) {
            firstRun = System.currentTimeMillis()
            installPref.edit().putLong("first_run", firstRun).apply()
        }
        val trialExpire = firstRun + TimeUnit.DAYS.toMillis(7)
        return System.currentTimeMillis() < trialExpire
    }

    fun getStatusText(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!prefs.contains("max_branches")) {
            return "TRIAL: 1 Branch"
        }
        val max = prefs.getInt("max_branches", 1)
        val left = getDaysLeft(context)
        if (isExpired(context)) return "EXPIRED! Max " + max
        return "LICENSED: " + max + " Branches, " + left + " days left"
    }

    fun blockClient(context: Context){
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("app_first_run", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
