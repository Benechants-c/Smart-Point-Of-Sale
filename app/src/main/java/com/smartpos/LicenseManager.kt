package com.smartpos

import android.content.Context

object LicenseManager {
    
    fun isActivated(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun canUseApp(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    // FIXED - Now returns Int to fix compareTo error!
    fun getDaysLeft(context: Context): Int {
        return FirebaseLicenseManager.getDaysLeft(context).toInt()
    }
    
    fun getStatusText(context: Context): String {
        if (!isActivated(context)) return "Trial Expired - Please Activate"
        val days = getDaysLeft(context)
        return when {
            days <= 0 -> "Expired - Renew now"
            days == 1 -> "Expires tomorrow!"
            days <= 3 -> "Expires in $days days"
            else -> "Active - $days days left"
        }
    }
    
    // FIXED - This was missing! Now added
    fun getMaxBranches(context: Context): Int {
        return 100 // Unlimited branches for 30-day license
    }
    
    fun getMaxUsers(context: Context): Int = 50
    fun getMaxShops(context: Context): Int = 100
    
    fun isLicenseValid(context: Context): Boolean = isActivated(context)
    fun checkLicense(context: Context): Boolean = isActivated(context)
    fun isExpired(context: Context): Boolean = !isActivated(context)
    fun getLicenseType(context: Context): String = "30-DAY"
    
    fun validateLicense(context: Context, code: String, callback: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(context, code, callback)
    }
}
