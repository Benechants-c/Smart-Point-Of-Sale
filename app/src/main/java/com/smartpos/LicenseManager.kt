package com.smartpos
import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object LicenseManager {
    private const val WORKER = "https://smartpo-licensess.benchatewa.workers.dev"
    private const val PREF = "SmartPOS_License"

    fun getDeviceId(c: Context): String {
        return try {
            Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) { "unknown_device" } ?: "unknown_device"
    }

    fun canUseApp(c: Context): Boolean {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return false
        val exp = p.getLong("expiry", 0L)
        return System.currentTimeMillis() <= exp
    }

    // ✅ FIXES YOUR NEW ERROR #381
    fun getDaysLeft(c: Context): Int {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return 0
        val exp = p.getLong("expiry", 0L)
        val diff = exp - System.currentTimeMillis()
        if (diff <= 0) return 0
        return (diff / 86400000L).toInt()
    }

    fun verifyLicense(context: Context, code: String, cb: (Boolean, String)->Unit) {
        Thread {
            try {
                val deviceId = getDeviceId(context)
                val cleanCode = code.trim().toUpperCase(Locale.ROOT)
                val fullUrl = WORKER + "/" + cleanCode + "?deviceId=" + deviceId
                val url = URL(fullUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val responseCode = conn.responseCode
                val input = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = input.bufferedReader()
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) { sb.append(line); line = reader.readLine() }
                reader.close()
                val text = sb.toString()
                val obj = JSONObject(text)
                val valid = obj.optBoolean("valid", false)
                if (valid) {
                    val days = obj.optInt("days", 30)
                    val branches = obj.optInt("branches", 3)
                    val expiry = obj.optLong("expiry", 0L)
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                        .putString("code", cleanCode)
                        .putInt("days", days)
                        .putInt("branches", branches)
                        .putLong("expiry", expiry)
                        .putString("deviceId", deviceId)
                        .putBoolean("active", true)
                        .apply()
                    cb(true, "✅ Activated! " + days + " days, " + branches + " shops")
                } else {
                    val err = obj.optString("error", "Invalid code")
                    cb(false, "❌ " + err)
                }
            } catch (e: Exception) {
                cb(false, "❌ Network error: " + e.message)
            }
        }.start()
    }

    fun getMaxBranches(c: Context): Int {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return 1
        if (System.currentTimeMillis() > p.getLong("expiry", 0L)) return 1
        return p.getInt("branches", 1)
    }

    fun getStatusText(c: Context): String {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return "No license - 1 branch demo - Buy: 0773996805"
        val exp = p.getLong("expiry", 0L)
        if (System.currentTimeMillis() > exp) return "EXPIRED - Buy new code: 0773996805"
        val daysLeft = getDaysLeft(c)
        val code = p.getString("code","")
        return "✅ Active: " + code + " - " + p.getInt("days",0) + " days - " + p.getInt("branches",0) + " shops - " + daysLeft + " days left"
    }
}