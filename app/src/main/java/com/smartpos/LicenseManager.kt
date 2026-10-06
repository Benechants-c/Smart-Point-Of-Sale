package com.smartpos

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.*

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private const val TAG = "LICENSE"
    
    // FIXED: Try default instance first, fallback to URL
    private fun getDb(): DatabaseReference {
        return try {
            FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            Log.e(TAG, "Default instance failed, using URL: ${e.message}")
            FirebaseDatabase.getInstance("https://smartpos-83781-default-rtdb.firebaseio.com/").reference
        }
    }
    
    private fun getDeviceId(c: Context): String {
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    fun isActivated(c: Context): Boolean {
        val p = c.getSharedPreferences("license", Context.MODE_PRIVATE)
        val valid = p.getBoolean("valid", false)
        val expiry = p.getLong("expiry", 0L)
        val lastCheck = p.getLong("last_check", 0L)
        val now = System.currentTimeMillis()
        
        if (lastCheck > 0 && now < lastCheck - 24*60*60*1000) {
            Log.w(TAG, "Time tampering detected!")
            return false
        }
        if (valid) p.edit().putLong("last_check", now).apply()
        return valid && now < expiry
    }

    fun getDaysLeft(c: Context): Int {
        val expiry = c.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry", 0L)
        val diff = expiry - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / (24*60*60*1000)).toInt()
    }
    
    fun getStatusText(c: Context): String {
        if (!isActivated(c)) return "❌ EXPIRED!\nContact: $CONTACT\nCall/Whatsapp/Text"
        return "✅ Active - ${getDaysLeft(c)} days left"
    }
    
    fun getMaxBranches(c: Context) = if (isActivated(c)) 100 else 0
    fun saveLicense(c: Context, code: String, expiry: Long) {
        c.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
            .putString("code",code)
            .putLong("expiry",expiry)
            .putBoolean("valid",true)
            .putLong("last_check", System.currentTimeMillis())
            .putLong("activated_at", System.currentTimeMillis())
            .apply()
        Log.i(TAG, "License saved: $code expiry $expiry")
    }

    fun activateLicense(c: Context, code: String): Boolean = false
    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = verifyLicense(c, code, cb)

    // FIXED VERIFY - handles String/Long and customer/customerName
    fun verifyLicense(c: Context, code: String, cb: (Boolean, String)->Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) { cb(false, "Enter code! Contact: $CONTACT"); return }
        val deviceId = getDeviceId(c)
        Log.i(TAG, "Checking code: $cleanCode device: $deviceId")

        getDb().child("licenses").child(cleanCode).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                Log.i(TAG, "Snapshot exists: ${snap.exists()} value: ${snap.value}")
                if (!snap.exists()) { 
                    cb(false, "❌ Invalid code! Contact: $CONTACT Call/Whatsapp/Text"); return 
                }
                
                // FIXED: Read expiry as Any then convert
                val expiryRaw = snap.child("expiry").value
                val expiry: Long = when(expiryRaw) {
                    is Long -> expiryRaw
                    is Number -> expiryRaw.toLong()
                    is String -> expiryRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                
                val used = snap.child("used").value as? Boolean ?: false
                val boundDevice = snap.child("deviceId").value as? String ?: ""
                // FIXED: support both customer and customerName
                val customer = snap.child("customerName").value as? String 
                    ?: snap.child("customer").value as? String 
                    ?: "Customer"
                
                Log.i(TAG, "Expiry raw: $expiryRaw -> $expiry, used: $used, device: $boundDevice")

                if (expiry == 0L) {
                    cb(false, "❌ Invalid expiry in Firebase! Contact $CONTACT")
                    return
                }
                if (System.currentTimeMillis() > expiry) { 
                    cb(false, "❌ EXPIRED! Paid days finished! Contact: $CONTACT"); return 
                }
                if (used && boundDevice.isNotEmpty() && boundDevice != deviceId) { 
                    cb(false, "❌ Code used on another phone! Contact: $CONTACT"); return 
                }
                if (!used) {
                    getDb().child("licenses").child(cleanCode).child("used").setValue(true)
                    getDb().child("licenses").child(cleanCode).child("deviceId").setValue(deviceId)
                    getDb().child("licenses").child(cleanCode).child("activatedAt").setValue(System.currentTimeMillis())
                }
                saveLicense(c, cleanCode, expiry)
                val days = (expiry - System.currentTimeMillis()) / (24*60*60*1000)
                cb(true, "✅ Welcome $customer! $days days left")
            }
            override fun onCancelled(e: DatabaseError) { 
                Log.e(TAG, "Firebase cancelled: ${e.message}")
                cb(false, "No internet! Need internet for first activation. ${e.message} Contact: $CONTACT") 
            }
        })
    }
    
    // Stubs for compatibility
    fun canUseApp(c: Context) = isActivated(c)
    fun isLicenseValid(c: Context) = isActivated(c)
    fun checkLicense(c: Context) = isActivated(c)
    fun isValid(c: Context) = isActivated(c)
    fun isExpired(c: Context) = !isActivated(c)
    fun getRemainingDays(c: Context) = getDaysLeft(c)
    fun getMaxBranch(c: Context) = getMaxBranches(c)
    fun getMaxUsers(c: Context) = 100
    fun getMaxShops(c: Context) = getMaxBranches(c)
    fun getMaxShop(c: Context) = getMaxBranches(c)
    fun getLicenseType(c: Context) = "30-DAY"
    fun getType(c: Context) = "30-DAY"
    fun getLicenseCode(c: Context) = c.getSharedPreferences("license", Context.MODE_PRIVATE).getString("code","")?:""
    fun getExpiryDate(c: Context) = c.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry",0L)
    fun generateLicense(c: Context) = ""
    fun createLicense(c: Context) = ""
    fun validateLicense(c: Context, code: String): Boolean = false
    fun validateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)
    fun activate(c: Context, code: String): Boolean = false
    fun activate(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)
}
