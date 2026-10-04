package com.smartpos

import android.content.Context
import java.util.concurrent.TimeUnit

object LicenseManager {
    private const val PREF = "license_prefs"
    private const val SECRET = "Chatewa2026SmartPOS" // SECRET KEY - never share with client
    
    // GENERATE CODE (only your Dev Tool uses this)
    fun generateCode(context: Context, branches: Int, days: Int): String {
        val bCode = when(branches){
            1->11; 3->33; 5->55; 10->101; 999->999; else->branches*11
        }
        // Strong checksum using SECRET
        val base = "$SECRET-$branches-$days"
        var hash = 0
        for(c in base){ hash = hash*31 + c.code }
        hash = kotlin.math.abs(hash) % 9000 + 1000 // 4 digit
        val hex = hash.toString(16).uppercase().padStart(4,'0')
        
        // Final code like CHT-55-30-A3F9-8K2P (hard to guess)
        val randomPart = (1000..9999).random().toString(36).uppercase()
        return "CHT-$bCode-$days-$hex-$randomPart"
    }

    // VALIDATE CODE - SECURE
    fun activateLicense(context: Context, code: String): Boolean {
        try{
            val clean = code.trim().uppercase()
            if(!clean.startsWith("CHT-")) return false
            val parts = clean.split("-")
            if(parts.size < 4) return false // Must be CHT-B-D-HASH-XXX
            
            val bCode = parts[1].toIntOrNull() ?: return false
            val days = parts[2].toIntOrNull() ?: return false
            val hashPart = parts[3]
            
            // Decode branches
            val branches = when(bCode){
                11->1; 33->3; 55->5; 101->10; 999->999
                else-> if(bCode % 11 == 0) bCode/11 else return false
            }
            
            // Recalculate hash and compare - THIS IS THE SECURITY
            val base = "$SECRET-$branches-$days"
            var hash = 0
            for(c in base){ hash = hash*31 + c.code }
            hash = kotlin.math.abs(hash) % 9000 + 1000
            val expectedHex = hash.toString(16).uppercase().padStart(4,'0')
            
            if(hashPart != expectedHex) return false // FAKE CODE REJECTED!
            
            // Valid - save it
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val expireTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            prefs.edit()
                .putInt("max_branches", branches)
                .putLong("expire_time", expireTime)
                .putInt("total_days", days)
                .putString("last_code", clean)
                .putLong("activated_at", System.currentTimeMillis())
                .apply()
            return true
        }catch(e:Exception){ return false }
    }

    fun getMaxBranches(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        // Trial: 1 branch only
        if(!prefs.contains("max_branches")) return 1
        // Check expiry
        if(isExpired(context)) return 0
        return prefs.getInt("max_branches", 1)
    }

    fun getDaysLeft(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if(!prefs.contains("expire_time")) return 7 // Trial 7 days
        val expire = prefs.getLong("expire_time", 0)
        val diff = expire - System.currentTimeMillis()
        return if(diff <=0) 0 else TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }

    fun isExpired(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if(!prefs.contains("expire_time")) return false // Trial not expired check separately
        return System.currentTimeMillis() > prefs.getLong("expire_time", 0)
    }

    fun canUseApp(context: Context): Boolean {
        // If license exists check expiry, else allow trial 7 days from first install
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if(prefs.contains("expire_time")){
            return !isExpired(context)
        }
        // Trial logic - first install time
        val installPref = context.getSharedPreferences("app_first_run", Context.MODE_PRIVATE)
        var firstRun = installPref.getLong("first_run", 0)
        if(firstRun == 0L){
            firstRun = System.currentTimeMillis()
            installPref.edit().putLong("first_run", firstRun).apply()
        }
        val trialDays = 7
        val trialExpire = firstRun + TimeUnit.DAYS.toMillis(trialDays.toLong())
        return System.currentTimeMillis() < trialExpire
    }

    fun getStatusText(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if(!prefs.contains("max_branches")){
            val trialLeft = 7 - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - context.getSharedPreferences("app_first_run", Context.MODE_PRIVATE).getLong("first_run", System.currentTimeMillis())).toInt()
            return "TRIAL: 1 Branch, $trialLeft days left"
        }
        val max = prefs.getInt("max_branches", 1)
        val left = getDaysLeft(context)
        if(isExpired(context)) return "❌ EXPIRED! Max $max | Buy new code"
        return "✅ LICENSED: $max Branches, $left days left"
    }

    fun blockClient(context: Context){
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("app_first_run", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
