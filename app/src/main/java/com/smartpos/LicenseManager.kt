package com.smartpos
import android.content.Context
object LicenseManager {
    fun isActivated(c: Context): Boolean = FirebaseLicenseManager.isActivated(c)
    fun canUseApp(c: Context): Boolean = FirebaseLicenseManager.isActivated(c)
    fun getDaysLeft(c: Context): Int = FirebaseLicenseManager.getDaysLeft(c).toInt()
    fun getStatusText(c: Context): String {
        if (!isActivated(c)) return "Expired - Activate"
        val d = getDaysLeft(c)
        return if (d <= 3) "⚠️ $d days left" else "✅ $d days left"
    }
    fun getMaxBranches(c: Context): Int = 100
    fun getMaxUsers(c: Context): Int = 50
    fun getMaxShops(c: Context): Int = 100
    fun isLicenseValid(c: Context): Boolean = isActivated(c)
    fun checkLicense(c: Context): Boolean = isActivated(c)
    fun isExpired(c: Context): Boolean = !isActivated(c)
    fun getLicenseType(c: Context): String = "30-DAY"
    // THIS FIXES YOUR ERROR at line 55:19
    fun activateLicense(c: Context, code: String, cb: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(c, code, cb)
    }
    fun validateLicense(c: Context, code: String, cb: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(c, code, cb)
    }
    fun activate(c: Context, code: String, cb: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(c, code, cb)
    }
    fun generateLicense(c: Context): String = ""
    fun saveLicense(c: Context, code: String, expiry: Long) {
        c.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
            .putString("code", code).putLong("expiry", expiry).putBoolean("valid", true).apply()
    }
}
