package com.smartpos
import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object LicenseManager {

    private const val PREFS = "license_prefs"
    private const val WORKER_URL = "https://smartpo-licensess.benchatewa-workflows-5d9.workers.dev"
    // ^^^ REPLACE with YOUR actual Worker URL if different!

    fun isLicenseValid(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = p.getLong("expiry_time", 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getStatusText(ctx: Context): String {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = p.getLong("expiry_time", 0L)
        val max = p.getInt("max_branches", 1)
        val shops = ctx.getSharedPreferences("shops_db", Context.MODE_PRIVATE).all.size
        if (expiry == 0L) return "❌ NO LICENSE - Activate"
        if (expiry < System.currentTimeMillis()) return "❌ EXPIRED! $shops/$max shops"
        val days = (expiry - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        return "✅ VALID: ${days+1} days left | $shops/$max shops"
    }

    fun getMaxBranches(ctx: Context): Int {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("max_branches", 1)
    }

    fun verifyLicense(ctx: Context, code: String, cb: (Boolean, String) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL("$WORKER_URL?code=$code")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                if (json.optBoolean("valid", false)) {
                    val days = json.optInt("days", 30)
                    val branches = json.optInt("branches", 5)
                    val expiry = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000

                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putLong("expiry_time", expiry)
                        .putInt("max_branches", branches)
                        .putString("license_code", code)
                        .apply()

                    cb(true, "✅ Welcome! $days days activated | Max $branches branches")
                } else {
                    cb(false, "❌ Invalid code: ${json.optString("message","Invalid")}")
                }
            } catch (e: Exception) {
                cb(false, "❌ Network error: ${e.message}\nTurn OFF WARP & try again")
            }
        }
    }
}