package com.smartpos

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LicenseManager {

    private const val PREFS_NAME = "license_prefs"
    private const val WORKER_URL = "https://smartpo-licenses.benchatewa-workflows-5d9.workers.dev"

    private fun getPrefs(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLicenseValid(ctx: Context): Boolean {
        val expiry = getPrefs(ctx).getLong("expiry_time", 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getMaxBranches(ctx: Context): Int {
        return getPrefs(ctx).getInt("max_branches", 1)
    }

    fun getStatusText(ctx: Context): String {
        val prefs = getPrefs(ctx)
        val expiry = prefs.getLong("expiry_time", 0L)
        val max = prefs.getInt("max_branches", 1)
        val shopsCount = ctx.getSharedPreferences("shops_db", Context.MODE_PRIVATE).all.size

        if (expiry == 0L) return "NO LICENSE - Activate"
        if (expiry < System.currentTimeMillis()) return "EXPIRED! $shopsCount/$max shops"
        val daysLeft = (expiry - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        return "VALID: ${daysLeft + 1} days left | $shopsCount/$max shops"
    }

    fun verifyLicense(ctx: Context, code: String, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val safeCode = URLEncoder.encode(code.trim(), "UTF-8")
                val fullUrl = "$WORKER_URL?code=$safeCode"
                val url = URL(fullUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                val input = if (responseCode == 200) conn.inputStream else conn.errorStream
                val responseText = input.bufferedReader().use { it.readText() }

                val json = JSONObject(responseText)
                val isValid = json.optBoolean("valid", false)

                if (isValid) {
                    val days = json.optInt("days", 30)
                    val branches = json.optInt("branches", 3)
                    val newExpiry = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L

                    getPrefs(ctx).edit()
                        .putLong("expiry_time", newExpiry)
                        .putInt("max_branches", branches)
                        .putString("license_code", code.trim())
                        .apply()

                    callback(true, "Activated! $days days | Max $branches branches")
                } else {
                    val msg = json.optString("message", "Invalid license")
                    callback(false, msg)
                }
            } catch (e: Exception) {
                callback(false, "Network error: " + e.message)
            }
        }.start()
    }
}