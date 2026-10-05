package com.smartpos

import android.content.Context

object LicenseManager {
    
    fun isActivated(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun canUseApp(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun getDaysLeft(context: Context): Long {
        return FirebaseLicenseManager.getDaysLeft(context)
    }
    
    fun getStatusText(context: Context): String {
        if (!isActivated(context)) {
            return "Trial Expired - Please Activate"
        }
        val days = getDaysLeft(context)
        return when {
            days <= 0 -> "Expired - Renew now"
            days == 1L -> "Expires tomorrow!"
            days <= 3L -> "Expires in $days days"
            else -> "Active - $days days left"
        }
    }
    
    fun isLicenseValid(context: Context): Boolean = isActivated(context)
    fun checkLicense(context: Context): Boolean = isActivated(context)
    fun isExpired(context: Context): Boolean = !isActivated(context)
    fun getLicenseType(context: Context): String = "30-DAY"
    
    fun validateLicense(context: Context, code: String, callback: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(context, code, callback)
    }
}
