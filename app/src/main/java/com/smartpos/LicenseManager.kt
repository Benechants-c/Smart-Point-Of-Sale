package com.smartpos
import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LicenseManager {
    private const val WORKER = "https://smartpo-licensess.benchatewa.workers.dev"
    private const val PREF = "SmartPOS_License"

    fun getDeviceId(c: Context): String {
        return try {
            Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        } catch (_: Exception) { "unknown_device" }
    }

    fun verifyLicense(context: Context, code: String, cb: (Boolean, String)->Unit) {
        Thread {
            try {
                val deviceId = getDeviceId(context)
                val fullUrl = "$WORKER/${code.trim().uppercase()}?deviceId=$deviceId"
                val url = URL(fullUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doInput = true
                
                val respCode = conn.responseCode
                val stream = if (respCode == 200) conn.inputStream else conn.errorStream
                val text = stream.bufferedReader().use { it.readText() }
                
                val obj = JSONObject(text)
                val valid = obj.optBoolean("valid", false)
                if (valid) {
                    val days = obj.optInt("days", 30)
                    val branches = obj.optInt("branches", 3)
                    val expiry = obj.optLong("expiry", 0L)
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                        .putString("code", code.trim().uppercase())
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
        val exp = p.getLong("expiry", 0L)
        if (System.currentTimeMillis() > exp) return 1
        return p.getInt("branches", 1)
    }

    fun getStatusText(c: Context): String {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return "No license - 1 branch demo - Buy: 0773996805"
        val exp = p.getLong("expiry", 0L)
        if (System.currentTimeMillis() > exp) return "EXPIRED - Buy new code: 0773996805"
        val daysLeft = ((exp - System.currentTimeMillis()) / 86400000L).toInt()
        val code = p.getString("code","")
        return "✅ Active: $code - ${p.getInt("days",0)} days - ${p.getInt("branches",0)} shops - $daysLeft days left"
    }
}