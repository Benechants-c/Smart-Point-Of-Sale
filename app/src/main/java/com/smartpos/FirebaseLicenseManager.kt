package com.smartpos

import android.content.Context
import android.provider.Settings
import com.google.firebase.database.*

object FirebaseLicenseManager {
    private val db = FirebaseDatabase.getInstance("https://smartpos-83781-default-rtdb.firebaseio.com/").reference

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // For POS app - ONLY VERIFY, NO GENERATE
    fun verifyLicense(context: Context, code: String, callback: (Boolean, String)->Unit) {
        val deviceId = getDeviceId(context)
        db.child("licenses").child(code).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (!snap.exists()) {
                    callback(false, "Invalid code!"); return
                }
                val used = snap.child("used").getValue(Boolean::class.java) ?: false
                val boundDevice = snap.child("deviceId").getValue(String::class.java) ?: ""
                val expiry = snap.child("expiry").getValue(Long::class.java) ?: 0L
                val customer = snap.child("customerName").getValue(String::class.java) ?: "Customer"

                if (System.currentTimeMillis() > expiry) {
                    callback(false, "License expired! Contact seller for renewal."); return
                }

                if (used) {
                    if (boundDevice == deviceId) {
                        // Same device - allow but check expiry
                        val daysLeft = (expiry - System.currentTimeMillis()) / (24*60*60*1000)
                        callback(true, "Welcome back $customer! $daysLeft days left")
                    } else {
                        callback(false, "Code already used on another device!")
                    }
                    return
                }

                // First time - lock to device
                db.child("licenses").child(code).child("used").setValue(true)
                db.child("licenses").child(code).child("deviceId").setValue(deviceId)
                db.child("licenses").child(code).child("activatedAt").setValue(System.currentTimeMillis())

                // Save locally
                context.getSharedPreferences("license", Context.MODE_PRIVATE).edit()
                    .putString("code", code)
                    .putLong("expiry", expiry)
                    .putBoolean("valid", true)
                    .apply()

                callback(true, "Activated! 30 days for $customer")
            }
            override fun onCancelled(e: DatabaseError) {
                callback(false, "Network error: ${e.message}")
            }
        })
    }

    fun isActivated(context: Context): Boolean {
        val pref = context.getSharedPreferences("license", Context.MODE_PRIVATE)
        val valid = pref.getBoolean("valid", false)
        val expiry = pref.getLong("expiry", 0L)
        return valid && System.currentTimeMillis() < expiry
    }

    fun getDaysLeft(context: Context): Long {
        val expiry = context.getSharedPreferences("license", Context.MODE_PRIVATE).getLong("expiry", 0L)
        return (expiry - System.currentTimeMillis()) / (24*60*60*1000)
    }
}
