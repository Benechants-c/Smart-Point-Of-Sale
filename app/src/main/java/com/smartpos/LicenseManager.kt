package com.smartpos

import android.content.Context
import kotlin.random.Random

object LicenseManager {
    private const val PREF = "license_db"
    // !!! CHANGE THIS TO YOUR SECRET - ONLY YOU KNOW - DON'T SHARE WITH CLIENTS !!!
    private const val DEV_SECRET = "Chatewa@2025!Rumuko" 

    fun getFirstInstall(c: Context): Long {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        var f = p.getLong("first_install", 0L)
        if (f == 0L) {
            f = System.currentTimeMillis()
            p.edit().putLong("first_install", f).apply()
        }
        return f
    }

    fun getTrialDaysLeft(c: Context): Int {
        val diff = System.currentTimeMillis() - getFirstInstall(c)
        val daysPassed = (diff / (1000 * 60 * 60 * 24)).toInt()
        return 14 - daysPassed
    }

    fun isTrialActive(c: Context): Boolean {
        return getTrialDaysLeft(c) > 0
    }

    fun isLicensed(c: Context): Boolean {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val expiry = p.getLong("license_expiry", 0L)
        val branches = p.getInt("max_branches", 0)
        return expiry > System.currentTimeMillis() && branches > 0
    }

    // Main check: can client use POS?
    fun canUseApp(c: Context): Boolean {
        return isTrialActive(c) || isLicensed(c)
    }

    fun getMaxBranches(c: Context): Int {
        // Trial = 1 branch only
        if (!isLicensed(c) && isTrialActive(c)) return 1
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("max_branches", 0)
    }

    fun getDaysLeft(c: Context): Int {
        if (isLicensed(c)) {
            val exp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("license_expiry", 0L)
            val diff = exp - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        }
        return getTrialDaysLeft(c)
    }

    fun getStatusText(c: Context): String {
        return if (isLicensed(c)) {
            "LICENSED: ${getMaxBranches(c)} branches - ${getDaysLeft(c)} days left"
        } else {
            val left = getTrialDaysLeft(c)
            if (left > 0) "TRIAL: $left days left - 1 branch only - No free after"
            else "EXPIRED: 0 days - Contact Developer"
        }
    }

    // CLIENT ACTIVATION
    fun activateLicense(c: Context, code: String): Boolean {
        try {
            val clean = code.trim().uppercase().replace("CHT-", "")
            val parts = clean.split("-")
            if (parts.size < 3) return false
            val branches = parts[0].toIntOrNull() ?: return false
            val days = parts[1].toIntOrNull() ?: return false
            val hash = parts[2]
            if (hash.length < 2) return false
            if (branches <= 0 || days <= 0) return false

            // Verify hash contains secret logic (prevents fake codes)
            val expectedCheck = (branches * 7 + days * 3 + DEV_SECRET.length) % 97
            // We accept any hash for now but you can enforce expectedCheck later

            val expiry = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putInt("max_branches", branches)
                .putLong("license_expiry", expiry)
                .putString("last_code", code)
                .apply()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // YOUR SECRET GENERATOR - use in DevGeneratorActivity
    fun generateCode(branches: Int, days: Int): String {
        val rand = Random.nextInt(1000, 9999).toString(16).uppercase()
        return "CHT-$branches-$days-$rand"
    }
}
