package com.smartpos

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

object LicenseManager {
    private const val CONTACT = "+263773996805"
    private const val TAG = "LICENSE"
    private const val PREFS = "license"
    private const val DB_URL = "https://smartpos-83781-default-rtdb.firebasedatabase.app"
    
    private fun getDb(context: Context): DatabaseReference {
        if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
        val db = FirebaseDatabase.getInstance(DB_URL)
        db.goOnline()
        return db.reference
    }

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
        val deviceId = getDeviceId(c)
        Log.i(TAG,"Verify $clean at $DB_URL")
        var finished = false
        fun done(ok:Boolean, msg:String){ if(finished) return; finished=true; android.os.Handler(android.os.Looper.getMainLooper()).post{ cb(ok,msg) } }

        // FORCE HTTP after 7 seconds if SDK stuck
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if(!finished){ Log.w(TAG,"SDK timeout -> HTTP"); httpVerify(c, clean, deviceId){ o,m -> done(o,m) } }
        }, 7000)

        try{
            getDb(c).child("licenses").child(clean).addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(s: DataSnapshot){
                    if(finished) return
                    Log.i(TAG,"Exists: ${s.exists()} Data: ${s.value}")
                    if(!s.exists()){ done(false,"❌ Invalid code! Contact: $CONTACT"); return }
                    var expiry = parseLong(s.child("expiry").value)
                    val days = parseLong(s.child("days").value)
                    val used = s.child("used").value as? Boolean ?: false
                    val bound = s.child("deviceId").value as? String ?: ""
                    val customer = s.child("customerName").value as? String ?: "Customer"
                    if(expiry==0L && days>0){ expiry=System.currentTimeMillis()+days*86400000L; getDb(c).child("licenses").child(clean).child("expiry").setValue(expiry) }
                    if(expiry==0L){ done(false,"No expiry days=$days"); return }
                    if(System.currentTimeMillis()>expiry){ done(false,"❌ EXPIRED! Contact: $CONTACT"); return }
                    if(used && bound.isNotEmpty() && bound!=deviceId && !clean.startsWith("TEST-")){ done(false,"❌ Used on another phone! $CONTACT"); return }
                    if(!used){ getDb(c).child("licenses").child(clean).updateChildren(mapOf("used" to true, "deviceId" to deviceId, "activatedAt" to System.currentTimeMillis(), "expiry" to expiry)) }
                    saveLicense(c, clean, expiry)
                    done(true,"✅ Welcome $customer! ${getDaysLeft(c)} days left")
                }
                override fun onCancelled(e: DatabaseError){ Log.e(TAG,"Cancelled ${e.message}"); if(!finished) httpVerify(c, clean, deviceId){ o,m -> done(o,m) } }
            })
        }catch(e:Exception){ Log.e(TAG,"Init fail ${e.message}"); if(!finished) httpVerify(c, clean, deviceId){ o,m -> done(o,m) } }
    }

    private fun httpVerify(c: Context, code:String, deviceId:String, cb:(Boolean,String)->Unit){
        Thread{
            try{
                val url = java.net.URL("$DB_URL/licenses/$code.json")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout=6000; conn.readTimeout=6000
                val txt = conn.inputStream.bufferedReader().readText()
                Log.i(TAG,"HTTP: $txt")
                if(txt.trim()=="null"){ cb(false,"❌ Invalid $code"); return@Thread }
                val days = Regex("\"days\"\\s*:\\s*(\\d+)").find(txt)?.groupValues?.get(1)?.toLongOrNull() ?: 14L
                val name = Regex("\"customerName\"\\s*:\\s*\"([^\"]+)\"").find(txt)?.groupValues?.get(1) ?: "Customer"
                val expiry = System.currentTimeMillis()+days*86400000L
                try{
                    var cc = java.net.URL("$DB_URL/licenses/$code/deviceId.json").openConnection() as java.net.HttpURLConnection
                    cc.requestMethod="PUT"; cc.doOutput=true; cc.outputStream.write("\"$deviceId\"".toByteArray()); cc.inputStream.close()
                    cc = java.net.URL("$DB_URL/licenses/$code/used.json").openConnection() as java.net.HttpURLConnection
                    cc.requestMethod="PUT"; cc.doOutput=true; cc.outputStream.write("true".toByteArray()); cc.inputStream.close()
                }catch(_:Exception){}
                android.os.Handler(android.os.Looper.getMainLooper()).post{ saveLicense(c, code, expiry); cb(true,"✅ Welcome $name! $days days (HTTP)") }
            }catch(e:Exception){ Log.e(TAG,"HTTP fail",e); cb(false,"Network error: ${e.message} Contact $CONTACT") }
        }.start()
    }

    fun activateLicense(c: Context, code: String, cb: (Boolean, String) -> Unit) = verifyLicense(c, code, cb)
    fun canUseApp(c: Context) = isActivated(c)
    fun isValid(c: Context) = isActivated(c)
    fun getRemainingDays(c: Context) = getDaysLeft(c)
    fun getMaxShops(c: Context) = getMaxBranches(c)
}