package com.smartpos

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private const val TAG = "LICENSE"
    private const val PREFS = "license"
    private const val WORKER_URL = "https://smartpo-licensess.benchatewa.workers.dev"

    private fun getDeviceId(c: Context): String = Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID)?: "unknown"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActivated(c: Context): Boolean {
        val p=prefs(c)
        val expiry = p.getLong("expiry",0L)
        if(expiry > 4100000000000L) return p.getBoolean("valid",false) // LIFETIME 2100
        return p.getBoolean("valid",false) && expiry > System.currentTimeMillis()
    }
    fun getDaysLeft(c: Context): Int {
        val expiry = prefs(c).getLong("expiry",0L)
        if(expiry > 4100000000000L) return 9999
        val d=expiry-System.currentTimeMillis()
        return if(d<=0)0 else (d/86400000L).toInt()+1
    }
    fun getStatusText(c: Context): String {
        if(!isActivated(c)) return "EXPIRED! Contact: $CONTACT"
        if(getDaysLeft(c) >= 9999) return "✅ LIFETIME LICENSE - PERMANENT"
        return "Active - ${getDaysLeft(c)} days"
    }
    fun getMaxBranches(c: Context): Int = prefs(c).getInt("branches", if(isActivated(c)) 100 else 0)
    fun getLicenseCode(c: Context): String = prefs(c).getString("code","")?:""

    fun saveLicense(c: Context, code: String, expiry: Long, branches: Int) {
        prefs(c).edit().putString("code",code).putLong("expiry",expiry).putBoolean("valid",true).putInt("branches", branches).apply()
        Log.i(TAG,"Saved $code $branches branches")
    }

    // --- NO LOCAL CODES - ALWAYS SERVER ---
    fun verifyLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) {
        val clean = code.trim().uppercase()
        if(clean.isBlank()){ cb(false,"Enter code!"); return }
        httpVerify(c, clean, getDeviceId(c), cb)
    }

    private fun httpVerify(c: Context, code:String, deviceId:String, cb:(Boolean,String)->Unit){
        Thread{
            try{
                val url = java.net.URL("$WORKER_URL/$code")
                Log.i(TAG,"Checking $url")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout=10000; conn.readTimeout=10000
                conn.setRequestProperty("Cache-Control","no-cache")
                val responseCode = conn.responseCode
                val txt = if(responseCode==200) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText()?: ""
                Log.i(TAG,"HTTP $responseCode: $txt")

                if(responseCode==404 || txt.contains("not found") || txt.trim()=="null"){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"Invalid code $code! Contact: $CONTACT") }
                    return@Thread
                }

                val json = JSONObject(txt)
                val valid = json.optBoolean("valid", true)
                if(!valid && !json.has("days") && !json.has("expiry")) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false, json.optString("message","Invalid code")) }
                    return@Thread
                }

                val days = json.optLong("days", 0L)
                val branches = json.optInt("branches", 3)
                val name = json.optString("customerName","Customer")
                var expiry = json.optLong("expiry",0L)
                val used = json.optBoolean("used",false)
                val bound = json.optString("deviceId","")
                val isLifetime = json.optBoolean("lifetime", false) || days >= 3650

                if(isLifetime) {
                    expiry = 4102444800000L // Jan 1, 2100 = LIFETIME
                } else if(expiry==0L && days>0){
                    expiry = System.currentTimeMillis() + days*86400000L
                }
                if(expiry==0L){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"No expiry set") }
                    return@Thread
                }
                if(!isLifetime && System.currentTimeMillis()>expiry){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"EXPIRED! Contact: $CONTACT") }
                    return@Thread
                }
                if(used && bound.isNotEmpty() && bound!=deviceId){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"Used on another phone! $CONTACT") }
                    return@Thread
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post{
                    saveLicense(c, code, expiry, branches)
                    val msg = if(isLifetime) "Welcome $name! LIFETIME LICENSE" else "Welcome $name! ${((expiry-System.currentTimeMillis())/86400000L).toInt()+1} days left"
                    cb(true, msg)
                }
            }catch(e:Exception){
                Log.e(TAG,"HTTP fail",e)
                android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"Network error: ${e.message} Contact $CONTACT") }
            }
        }.start()
    }

    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = verifyLicense(c, code, cb)
    fun canUseApp(c: Context) = isActivated(c)
    fun isValid(c: Context) = isActivated(c)
    fun getRemainingDays(c: Context) = getDaysLeft(c)
    fun getMaxShops(c: Context) = getMaxBranches(c)
    fun getExpiryDate(c: Context): String {
        val expiry = prefs(c).getLong("expiry",0L)
        if(expiry > 4100000000000L) return "2099-12-31 LIFETIME"
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(expiry))
    }
}