package com.smartpos

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

object LicenseManager {

    private const val PREFS = "license_prefs"
    // FIXED URL - yours had double S "licensess"
    private const val WORKER_URL = "https://smartpo-licenses.benchatewa-workflows-5d9.workers.dev"

    fun isLicenseValid(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = p.getLong("expiry_time", 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getMaxBranches(ctx: Context): Int {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("max_branches", 1)
    }

    fun getStatusText(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = prefs.getLong("expiry_time", 0L)
        val max = prefs.getInt("max_branches", 1)
        val shops = ctx.getSharedPreferences("shops_db", Context.MODE_PRIVATE).all.size

        if (expiry == 0L) return "❌ NO LICENSE - Activate"
        if (expiry < System.currentTimeMillis()) return "❌ EXPIRED! $shops/$max shops"
        val daysLeft = (expiry - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        return "✅ VALID: ${daysLeft + 1} days left | $shops/$max shops"
    }

    fun verifyLicense(ctx: Context, code: String, cb: (Boolean, String) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val safeCode = URLEncoder.encode(code.trim(), "UTF-8")
                val url = URL("$WORKER_URL?code=$safeCode")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "SmartPOS")

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().readText()
                
                val json = JSONObject(response)

                if (json.optBoolean("valid", false)) {
                    val days = json.optInt("days", 30)
                    val branches = json.optInt("branches", 5)
                    val expiry = System.currentTimeMillis() + days * 24L * 60L * 1000L

                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putLong("expiry_time", expiry)
                        .putInt("max_branches", branches)
                        .putString("license_code", code.trim())
                        .apply()

                    cb(true, "✅ Activated! $days days | Max $branches branches")
                } else {
                    cb(false, "❌ ${json.optString("message", "Invalid license")}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cb(false, "❌ Network error: ${e.message}")
            }
        }
    }
}