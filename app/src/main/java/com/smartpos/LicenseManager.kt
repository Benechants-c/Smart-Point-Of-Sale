package com.smartpos
import android.content.Context
import java.util.Calendar
import kotlin.random.Random
object LicenseManager {
    private const val PREF = "license_db"
    fun getFirstInstall(c: Context): Long {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        var f = p.getLong("first_install", 0L)
        if (f == 0L) { f = System.currentTimeMillis(); p.edit().putLong("first_install", f).apply() }
        return f
    }
    fun getTrialDaysLeft(c: Context): Int {
        val diff = System.currentTimeMillis() - getFirstInstall(c)
        return 14 - (diff / (1000*60*60*24)).toInt()
    }
    fun isTrialActive(c: Context): Boolean = getTrialDaysLeft(c) > 0
    fun isLicensed(c: Context): Boolean {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return System.currentTimeMillis() < p.getLong("license_expiry", 0L) && p.getInt("max_branches", 0) > 0
    }
    fun canUseApp(c: Context): Boolean = isTrialActive(c) || isLicensed(c)
    fun getMaxBranches(c: Context): Int {
        if (!isLicensed(c) && isTrialActive(c)) return 1
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("max_branches", 0)
    }
    fun getDaysLeft(c: Context): Int {
        if (isLicensed(c)) {
            val exp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("license_expiry", 0L)
            return ((exp - System.currentTimeMillis()) / (1000*60*60*24)).toInt().coerceAtLeast(0)
        }
        return getTrialDaysLeft(c)
    }
    fun getStatusText(c: Context): String {
        return if (isLicensed(c)) "LICENSED: ${getMaxBranches(c)} branches - ${getDaysLeft(c)} days left"
        else { val left=getTrialDaysLeft(c); if(left>0) "TRIAL: $left days left - 1 branch only" else "EXPIRED: 0 days - Activate license" }
    }
    fun activateLicense(c: Context, code: String): Boolean {
        try {
            val clean = code.trim().uppercase().replace("CHT-", "")
            val parts = clean.split("-")
            if (parts.size < 3) return false
            val branches = parts[0].toIntOrNull()?: return false
            val days = parts[1].toIntOrNull()?: return false
            val expiry = System.currentTimeMillis() + days*24L*60*60*1000
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("max_branches", branches).putLong("license_expiry", expiry).putString("last_code", code).apply()
            return true
        } catch (e: Exception) { return false }
    }
    fun generateCode(branches: Int, days: Int): String {
        val rand = Random.nextInt(1000, 9999).toString(16).uppercase()
        return "CHT-$branches-$days-$rand"
    }
    // LEGACY - fixes old activities
    fun daysLeft(c: Context): Int = getDaysLeft(c)
    fun isExpired(c: Context): Boolean =!canUseApp(c)
    fun shouldShowWarning(c: Context): Boolean = getDaysLeft(c) in 1..5
    fun getDaysInCurrentMonth(): Int { val cal=Calendar.getInstance(); return cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    fun reactivateClient(c: Context, code: String): Boolean = activateLicense(c, code)
    fun reactivateClient(c: Context, branches: Int): Boolean {
        val code = generateCode(branches, 365)
        return activateLicense(c, code)
    }
}
