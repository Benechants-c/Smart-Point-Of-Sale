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
        return p.getBoolean("valid", false) && System.currentTimeMillis() < p.getLong("expiry", 0L)
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
            .putString("code",code).putLong("expiry",expiry).putBoolean("valid",true).apply()
    }

    // === PARSE DAYS FROM CODE: CHATEWA-30 = 30 days, CHATEWA-90 = 90 days ===
    private fun parseDaysFromCode(code: String): Int {
        return try {
            val regex = Regex("""(\d+)""")
            regex.findAll(code).lastOrNull()?.value?.toInt() ?: 30
        } catch (e: Exception) { 30 }
    }

    // === SYNC - FIXES ManageShopsActivity:55 - NOW RESPECTS PAID DAYS ===
    fun activateLicense(c: Context, code: String): Boolean {
        if (code.isBlank()) return false
        val days = parseDaysFromCode(code)
        if (days <= 0 || days > 3650) return false
        val expiry = System.currentTimeMillis() + days.toLong() * 24 * 60 * 60 * 1000
        saveLicense(c, code, expiry)
        return true
    }

    // === ASYNC ===
    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) {
        verifyLicense(c, code, cb)
    }
    fun validateLicense(c: Context, code: String): Boolean = activateLicense(c, code)
    fun validateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)
    fun activate(c: Context, code: String): Boolean = activateLicense(c, code)
    fun activate(c: Context, code: String, cb: (Boolean, String) -> Unit) = activateLicense(c, code, cb)

    fun verifyLicense(c: Context, code: String, cb: (Boolean, String)->Unit) {
        val deviceId = getDeviceId(c)
        val daysFromCode = parseDaysFromCode(code)
        
        getDb().child("licenses").child(code).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists()) {
                    val used = snap.child("used").getValue(Boolean::class.java) ?: false
                    val boundDevice = snap.child("deviceId").getValue(String::class.java) ?: ""
                    val expiry = snap.child("expiry").getValue(Long::class.java) ?: 0L
                    val customer = snap.child("customerName").getValue(String::class.java) ?: "Customer"
                    if (System.currentTimeMillis() > expiry) { 
                        cb(false, "❌ EXPIRED! Contact: $CONTACT Call/Whatsapp/Text"); return 
                    }
                    if (used && boundDevice != deviceId) { 
                        cb(false, "Used on another device! Contact: $CONTACT"); return 
                    }
                    if (!used) {
                        getDb().child("licenses").child(code).child("used").setValue(true)
                        getDb().child("licenses").child(code).child("deviceId").setValue(deviceId)
                        getDb().child("licenses").child(code).child("activatedAt").setValue(System.currentTimeMillis())
                    }
                    saveLicense(c, code, expiry)
                    val days = (expiry - System.currentTimeMillis()) / (24*60*60*1000)
                    cb(true, "Welcome $customer! $days days left")
                } else {
                    // Offline activation using days in code (for your testing/business)
                    if (daysFromCode > 0) {
                        val expiry = System.currentTimeMillis() + daysFromCode.toLong() * 24 * 60 * 60 * 1000
                        saveLicense(c, code, expiry)
                        cb(true, "✅ Activated! $daysFromCode days")
                    } else {
                        cb(false, "❌ Invalid code! Contact: $CONTACT")
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {
                // No internet - allow offline with days from code
                if (daysFromCode > 0) {
                    val expiry = System.currentTimeMillis() + daysFromCode.toLong() * 24 * 60 * 60 * 1000
                    saveLicense(c, code, expiry)
                    cb(true, "✅ Activated Offline! $daysFromCode days")
                } else {
                    cb(false, "Network: ${e.message} Contact: $CONTACT")
                }
            }
        })
    }
}
