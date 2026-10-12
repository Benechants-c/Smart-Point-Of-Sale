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
        val p=prefs(c);
        return p.getBoolean("valid",false) && p.getLong("expiry",0L) > System.currentTimeMillis()
    }
    fun getDaysLeft(c: Context): Int {
        val d=prefs(c).getLong("expiry",0L)-System.currentTimeMillis();
        return if(d<=0)0 else (d/86400000L).toInt()+1
    }
    fun getStatusText(c: Context): String = if(!isActivated(c)) "EXPIRED! Contact: $CONTACT" else "Active - ${getDaysLeft(c)} days"
    fun getMaxBranches(c: Context): Int = prefs(c).getInt("branches", if(isActivated(c)) 100 else 0)
    fun getLicenseCode(c: Context): String = prefs(c).getString("code","")?:""

    fun saveLicense(c: Context, code: String, expiry: Long, branches: Int) {
        prefs(c).edit().putString("code",code).putLong("expiry",expiry).putBoolean("valid",true).putInt("branches", branches).apply()
        Log.i(TAG,"Saved $code $branches branches")
    }

    fun verifyLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) {
        val clean = code.trim().uppercase()
        if(clean.isBlank()){ cb(false,"Enter code!"); return }

        // --- LOCAL 14 / 30 DAY TESTS - No internet needed, always works ---
        val localLic = when(clean) {
            "TEST-14" -> Pair(14L, 3)
            "TEST-30" -> Pair(30L, 3)
            "SMART-14" -> Pair(14L, 3)
            "SMART-30" -> Pair(30L, 3)
            "CHT-3-30-XXX" -> Pair(30L, 3)
            "SMART-90" -> Pair(90L, 5)
            "SMART-365" -> Pair(365L, 10)
            else -> null
        }
        if(localLic!= null) {
            val expiry = System.currentTimeMillis() + localLic.first * 86400000L
            saveLicense(c, clean, expiry, localLic.second)
            cb(true, "Welcome! ${localLic.first} days | ${localLic.second} branches")
            return
        }

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
                // support both formats: {valid, days} and {days, expiry}
                val valid = json.optBoolean("valid", true)
                if(!valid &&!json.has("days")) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false, json.optString("message","Invalid code")) }
                    return@Thread
                }

                val days = json.optLong("days", 0L)
                val branches = json.optInt("branches", 3)
                val name = json.optString("customerName","Customer")
                var expiry = json.optLong("expiry",0L)
                val used = json.optBoolean("used",false)
                val bound = json.optString("deviceId","")

                if(expiry==0L && days>0){
                    expiry = System.currentTimeMillis() + days*86400000L
                }
                if(expiry==0L){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"No expiry set") }
                    return@Thread
                }
                if(System.currentTimeMillis()>expiry){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"EXPIRED! Contact: $CONTACT") }
                    return@Thread
                }
                if(used && bound.isNotEmpty() && bound!=deviceId &&!code.startsWith("TEST-")){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"Used on another phone! $CONTACT") }
                    return@Thread
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post{
                    saveLicense(c, code, expiry, branches)
                    cb(true,"Welcome $name! ${((expiry-System.currentTimeMillis())/86400000L).toInt()+1} days left")
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
}