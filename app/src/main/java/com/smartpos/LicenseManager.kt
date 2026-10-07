package com.smartpos

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private const val TAG = "LICENSE"
    private const val PREFS = "license"
    private const val DB_URL = "https://smartpos-83781-default-rtdb.firebaseio.com"
    
    private fun getDb(context: Context): DatabaseReference {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        return FirebaseDatabase.getInstance(DB_URL).reference
    }

    private fun getDeviceId(c: Context): String {
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActivated(c: Context): Boolean {
        val p = prefs(c)
        val valid = p.getBoolean("valid", false)
        val expiry = p.getLong("expiry", 0L)
        if (!valid || expiry == 0L) return false
        return System.currentTimeMillis() <= expiry
    }

    fun getDaysLeft(c: Context): Int {
        val expiry = prefs(c).getLong("expiry", 0L)
        val diff = expiry - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / 86400000L).toInt() + 1
    }

    fun getStatusText(c: Context): String {
        return if (!isActivated(c)) "❌ EXPIRED!\nContact: $CONTACT"
        else "✅ Active - ${getDaysLeft(c)} days left"
    }

    fun getMaxBranches(c: Context): Int = if (isActivated(c)) 100 else 0
    fun getLicenseCode(c: Context): String = prefs(c).getString("code", "") ?: ""

    fun saveLicense(c: Context, code: String, expiry: Long) {
        prefs(c).edit()
            .putString("code", code)
            .putLong("expiry", expiry)
            .putBoolean("valid", true)
            .putLong("last_check", System.currentTimeMillis())
            .apply()
        Log.i(TAG, "Saved: $code -> $expiry")
    }

    private fun parseLong(v: Any?): Long {
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    fun verifyLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            cb(false, "Enter code! Contact: $CONTACT")
            return
        }
        
        val deviceId = getDeviceId(c)
        Log.i(TAG, "Verifying $cleanCode device $deviceId")

        try {
            getDb(c).child("licenses").child(cleanCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    Log.i(TAG, "Exists: ${snap.exists()} Data: ${snap.value}")
                    
                    if (!snap.exists()) {
                        cb(false, "❌ Invalid code! Contact: $CONTACT")
                        return
                    }

                    val days = parseLong(snap.child("days").value)
                    val used = snap.child("used").value as? Boolean ?: false
                    val boundDevice = snap.child("deviceId").value as? String ?: snap.child("device_id").value as? String ?: ""
                    val customer = snap.child("customerName").value as? String ?: snap.child("customer").value as? String ?: "Customer"
                    var expiry = parseLong(snap.child("expiry").value)

                    if (expiry == 0L && days > 0) {
                        expiry = System.currentTimeMillis() + (days * 86400000L)
                        getDb(c).child("licenses").child(cleanCode).child("expiry").setValue(expiry)
                    }

                    if (expiry == 0L) {
                        cb(false, "❌ License has no expiry! Contact $CONTACT (days=$days)")
                        return
                    }
                    
                    if (System.currentTimeMillis() > expiry) {
                        cb(false, "❌ EXPIRED! Contact: $CONTACT")
                        return
                    }

                    if (used && boundDevice.isNotEmpty() && boundDevice != deviceId) {
                        if (!cleanCode.startsWith("TEST-")) {
                           cb(false, "❌ Code used on another phone! Contact: $CONTACT")
                           return
                        }
                    }

                    if (!used) {
                        val updates = mapOf<String, Any>(
                            "used" to true,
                            "deviceId" to deviceId,
                            "activatedAt" to System.currentTimeMillis(),
                            "expiry" to expiry
                        )
                        getDb(c).child("licenses").child(cleanCode).updateChildren(updates)
                    }

                    saveLicense(c, cleanCode, expiry)
                    val left = ((expiry - System.currentTimeMillis()) / 86400000L).toInt() + 1
                    cb(true, "✅ Welcome $customer! $left days left")
                }

                override fun onCancelled(e: DatabaseError) {
                    Log.e(TAG, "Firebase cancelled: ${e.code} ${e.message}")
                    cb(false, "DB Error ${e.code}: ${e.message}\nCheck Rules! Contact: $CONTACT")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
            cb(false, "Firebase init failed: ${e.message}")
        }
    }

    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = verifyLicense(c, code, cb)
    fun canUseApp(c: Context) = isActivated(c)
    fun isValid(c: Context) = isActivated(c)
    fun getRemainingDays(c: Context) = getDaysLeft(c)
    fun getMaxShops(c: Context) = getMaxBranches(c)
}
