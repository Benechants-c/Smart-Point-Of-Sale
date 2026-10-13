package com.smartpos
import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.URL

object LicenseManager {
    private const val WORKER = "https://smartpo-licensess.benchatewa.workers.dev"
    private const val PREF = "SmartPOS_License"

    private fun getDeviceId(c: Context): String {
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    fun verifyLicense(context: Context, code: String, cb: (Boolean, String)->Unit) {
        Thread {
            try {
                val deviceId = getDeviceId(context)
                val url = "$WORKER/${code.uppercase()}?deviceId=$deviceId"
                val resp = URL(url).readText()
                val obj = JSONObject(resp)
                val valid = obj.getBoolean("valid")
                if (valid) {
                    val days = obj.getInt("days")
                    val branches = obj.getInt("branches")
                    val expiry = obj.getLong("expiry")
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                        .putString("code", code.uppercase())
                        .putInt("days", days)
                        .putInt("branches", branches)
                        .putLong("expiry", expiry)
                        .putString("deviceId", deviceId)
                        .putBoolean("active", true)
                        .apply()
                    cb(true, "✅ Activated! $days days, $branches shops")
                } else {
                    val err = obj.optString("error", "Invalid code")
                    cb(false, "❌ $err")
                }
            } catch (e: Exception) {
                cb(false, "❌ Network error: ${e.message}")
            }
        }.start()
    }

    fun getMaxBranches(c: Context): Int {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return 1
        if (System.currentTimeMillis() > p.getLong("expiry", 0)) return 1
        return p.getInt("branches", 1)
    }

    fun getStatusText(c: Context): String {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return "No license - 1 branch demo - Buy: 0773996805"
        val exp = p.getLong("expiry", 0)
        if (System.currentTimeMillis() > exp) return "EXPIRED - Buy new code: 0773996805"
        val daysLeft = ((exp - System.currentTimeMillis()) / 86400000).toInt()
        return "✅ Active: ${p.getString("code","")} - ${p.getInt("days",0)} days - ${p.getInt("branches",0)} shops - $daysLeft days left"
    }
}