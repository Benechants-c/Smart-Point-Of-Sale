package com.smartpos

import android.content.Context
import android.provider.Settings
import com.google.firebase.database.*

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private fun getDb() = FirebaseDatabase.getInstance("https://smartpos-83781-default-rtdb.firebaseio.com/").reference
    private fun getDeviceId(c: Context): String {
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    fun isActivated(c: Context): Boolean {
        val p = c.getSharedPreferences("license", Context.MODE_PRIVATE)
        val valid = p.getBoolean("valid", false)
        val expiry = p.getLong("expiry", 0L)
        val lastCheck = p.getLong("last_check", 0L)
        val now = System.currentTimeMillis()
        
        // BYPASS FIX 2: Detect time tampering - if phone time went backwards, expire!
        if (lastCheck > 0 && now < lastCheck - 24*60*60*1000) {
            return false // Phone time manipulated backwards!
        }
        // Save last check time
        if (valid) p.edit().putLong("last_check", now).apply()
        
        return valid && now < expiry
    }

    fun canUseApp(c: Context) = isActivated(c)
    fun isLicenseValid(c: Context) = isActivated(c)
    fun checkLicense(c: Context) = isActivated(c)
    fun isValid(c: Context) = isActivated(c)
    fun isExpired(c: Context) = !isActivated(c)
    fun getDaysLeft(c: Context): Int {
        val expiry = c.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry", 0L)
        val diff = expiry - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / (24*60*60*1000)).toInt()
    }
    fun getStatusText(c: Context): String {
        if (!isActivated(c)) return "❌ EXPIRED!\nContact: $CONTACT\nCall/Whatsapp/Text"
        return "✅ Active - ${getDaysLeft(c)} days left"
    }
    fun getRemainingDays(c: Context) = getDaysLeft(c)
    fun getMaxBranches(c: Context) = 100
    fun getMaxBranch(c: Context) = 100
    fun getMaxUsers(c: Context) = 100
    fun getMaxShops(c: Context) = 100
    fun getMaxShop(c: Context) = 100
    fun getLicenseType(c: Context) = "30-DAY"
    fun getType(c: Context) = "30-DAY"
    fun getLicenseCode(c: Context) = c.getSharedPreferences("license", Context.MODE_PRIVATE).getString("code","")?:""
    fun getExpiryDate(c: Context) = c.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry",0L)
    fun generateLicense(c: Context) = ""
    fun createLicense(c: Context) = ""
    
    fun saveLicense(c: Context, code: String, expiry: Long) {
        c.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
            .putString("code",code)
            .putLong("expiry",expiry)
            .putBoolean("valid",true)
            .putLong("last_check", System.currentTimeMillis())
            .putLong("activated_at", System.currentTimeMillis())
            .apply()
    }

    // BYPASS FIX 1: NO OFFLINE ACTIVATION - Must be in Firebase!
    // This version BLOCKS fake codes - returns false if not in Firebase
    fun activateLicense(c: Context, code: String): Boolean {
        // Sync version now BLOCKS offline bypass - only for internal use
        // Real activation must go through verifyLicense() which checks Firebase
        return false // Force use of async Firebase verification
    }

    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) {
        verifyLicense(c, code, cb)
    }
    fun validateLicense(c: Context, code: String): Boolean = false
    fun validateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)
    fun activate(c: Context, code: String): Boolean = false
    fun activate(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)

    // SECURE VERIFY - NO BYPASS
    fun verifyLicense(c: Context, code: String, cb: (Boolean, String)->Unit) {
        if (code.isBlank()) { cb(false, "Enter code! Contact: $CONTACT"); return }
        val deviceId = getDeviceId(c)

        getDb().child("licenses").child(code).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (!snap.exists()) { 
                    cb(false, "❌ Invalid code! Contact: $CONTACT Call/Whatsapp/Text"); return 
                }
                val used = snap.child("used").getValue(Boolean::class.java) ?: false
                val boundDevice = snap.child("deviceId").getValue(String::class.java) ?: ""
                val expiry = snap.child("expiry").getValue(Long::class.java) ?: 0L
                val customer = snap.child("customerName").getValue(String::class.java) ?: "Customer"
                
                if (System.currentTimeMillis() > expiry) { 
                    cb(false, "❌ EXPIRED! Paid days finished! Contact: $CONTACT"); return 
                }
                // BYPASS FIX 3: Device lock - one code = one phone only!
                if (used && boundDevice.isNotEmpty() && boundDevice != deviceId) { 
                    cb(false, "❌ Code used on another phone! Contact: $CONTACT"); return 
                }
                if (!used) {
                    getDb().child("licenses").child(code).child("used").setValue(true)
                    getDb().child("licenses").child(code).child("deviceId").setValue(deviceId)
                    getDb().child("licenses").child(code).child("activatedAt").setValue(System.currentTimeMillis())
                }
                saveLicense(c, code, expiry)
                val days = (expiry - System.currentTimeMillis()) / (24*60*60*1000)
                cb(true, "✅ Welcome $customer! $days days left (Paid days respected)")
            }
            override fun onCancelled(e: DatabaseError) { 
                cb(false, "No internet! Need internet for first activation. Contact: $CONTACT") 
            }
        })
    }
}
