package com.smartpos

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private const val TAG = "LICENSE"
    private const val PREFS = "license"
    // ✅ TELONE-PROOF - Cloudflare Worker, not Firebase directly
    private const val WORKER_URL = "https://smartpo-licensess.benchatewa.workers.dev"
    
    private fun getDeviceId(c: Context): String = Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isActivated(c: Context): Boolean { val p=prefs(c); return p.getBoolean("valid",false) && p.getLong("expiry",0L) > System.currentTimeMillis() }
    fun getDaysLeft(c: Context): Int { val d=prefs(c).getLong("expiry",0L)-System.currentTimeMillis(); return if(d<=0)0 else (d/86400000L).toInt()+1 }
    fun getStatusText(c: Context): String = if(!isActivated(c)) "❌ EXPIRED!\nContact: $CONTACT" else "✅ Active - ${getDaysLeft(c)} days"
    fun getMaxBranches(c: Context): Int = if(isActivated(c)) 100 else 0
    fun getLicenseCode(c: Context): String = prefs(c).getString("code","")?:""
    fun saveLicense(c: Context, code: String, expiry: Long) { prefs(c).edit().putString("code",code).putLong("expiry",expiry).putBoolean("valid",true).apply(); Log.i(TAG,"Saved $code") }
    private fun parseLong(v: Any?): Long = when(v){ is Number -> v.toLong(); is String -> v.toLongOrNull()?:0L; else -> 0L }

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
                val txt = if(responseCode==200) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.i(TAG,"HTTP $responseCode: $txt")
                
                if(responseCode==404 || txt.contains("not found") || txt.trim()=="null"){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"❌ Invalid code $code! Contact: $CONTACT") }
                    return@Thread
                }
                
                val json = JSONObject(txt)
                val days = json.optLong("days", 0L)
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
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"❌ EXPIRED! Contact: $CONTACT") }
                    return@Thread
                }
                if(used && bound.isNotEmpty() && bound!=deviceId && !code.startsWith("TEST-")){
                    android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(false,"❌ Used on another phone! $CONTACT") }
                    return@Thread
                }
                
                // Activate - PUT back via Worker
                try{
                    val update = JSONObject()
                    update.put("days", days)
                    update.put("customerName", name)
                    update.put("deviceId", deviceId)
                    update.put("used", true)
                    update.put("activatedAt", System.currentTimeMillis())
                    update.put("expiry", expiry)
                    
                    val putConn = java.net.URL("$WORKER_URL/$code").openConnection() as java.net.HttpURLConnection
                    putConn.requestMethod="PUT"
                    putConn.doOutput=true
                    putConn.setRequestProperty("Content-Type","application/json")
                    putConn.connectTimeout=10000
                    putConn.outputStream.write(update.toString().toByteArray())
                    val putCode = putConn.responseCode
                    Log.i(TAG,"Activate PUT $putCode")
                    putConn.inputStream.close()
                }catch(e:Exception){ Log.e(TAG,"Activate write failed",e) }
                
                android.os.Handler(android.os.Looper.getMainLooper()).post{
                    saveLicense(c, code, expiry)
                    cb(true,"✅ Welcome $name! ${((expiry-System.currentTimeMillis())/86400000L).toInt()+1} days left")
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
