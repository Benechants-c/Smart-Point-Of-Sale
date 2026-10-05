package com.smartpos

import android.content.Context

// WRAPPER - Fixes build errors! Uses new Firebase system inside
object LicenseManager {
    
    fun isActivated(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun isLicenseValid(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun checkLicense(context: Context): Boolean {
        return FirebaseLicenseManager.isActivated(context)
    }
    
    fun getDaysLeft(context: Context): Long {
        return FirebaseLicenseManager.getDaysLeft(context)
    }
    
    fun getExpiryDate(context: Context): Long {
        return context.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry", 0L)
    }
    
    fun isExpired(context: Context): Boolean {
        return !isActivated(context)
    }
    
    // For old code that had license type checks
    fun getLicenseType(context: Context): String {
        return "30-DAY"
    }
    
    fun validateLicense(context: Context, code: String, callback: (Boolean, String)->Unit) {
        FirebaseLicenseManager.verifyLicense(context, code, callback)
    }
}
