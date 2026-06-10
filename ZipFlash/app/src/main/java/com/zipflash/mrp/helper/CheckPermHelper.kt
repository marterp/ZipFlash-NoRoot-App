package com.zipflash.mrp.helper

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object CheckPermHelper {
    private var hideLimitedWarning = false
    private var skipShizukuTransient = false

    fun getHideLimitedWarning(): Boolean = hideLimitedWarning

    fun setHideLimitedWarning(hide: Boolean) {
        hideLimitedWarning = hide
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            rikka.shizuku.Shizuku.getVersion() > 0
                && rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun hasWriteSecureSettings(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    }

    fun isSkipShizukuPersisted(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getBoolean("skip_shizuku", false)
    }

    fun isSkipShizukuTransient(): Boolean = skipShizukuTransient

    fun isSkipShizuku(ctx: Context): Boolean = isSkipShizukuTransient() || isSkipShizukuPersisted(ctx)

    fun setSkipShizukuPersisted(ctx: Context, value: Boolean) {
        val prefs = ctx.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("skip_shizuku", value).apply()
    }

    fun setSkipShizukuTransient(value: Boolean) {
        skipShizukuTransient = value
    }

    fun resetSkipFlags(ctx: Context) {
        skipShizukuTransient = false
        val prefs = ctx.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().remove("skip_shizuku").apply()
    }
}
