package com.zipflash.mrp.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

/**
 * Helper class for checking Shizuku and WRITE_SECURE_SETTINGS permissions,
 * and managing skip-state logic for limited mode.
 */
public class CheckPermHelper {
	
	// --- Temporary "don't show again" flag (resets when app restarts) ---
	private static boolean hideLimitedWarning = false;

	public static boolean getHideLimitedWarning() {
		return hideLimitedWarning;
	}

	public static void setHideLimitedWarning(boolean hide) {
		hideLimitedWarning = hide;
	}

    // Temporary in-memory skip (resets when app restarts)
    private static boolean skipShizukuTransient = false;

    // Check if Shizuku permission is granted
    public static boolean hasShizukuPermission() {
        try {
            return (rikka.shizuku.Shizuku.getVersion() > 0)
				&& (rikka.shizuku.Shizuku.checkSelfPermission()
				== PackageManager.PERMISSION_GRANTED);
        } catch (Throwable e) {
            return false;
        }
    }

    // Check if WRITE_SECURE_SETTINGS is granted
    public static boolean hasWriteSecureSettings(Context ctx) {
        return ContextCompat.checkSelfPermission(
			ctx, "android.permission.WRITE_SECURE_SETTINGS")
			== PackageManager.PERMISSION_GRANTED;
    }

    // Persistent skip flag ("Don't show again" until app restart)
    public static boolean isSkipShizukuPersisted(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean("skip_shizuku", false);
    }

    // Transient skip (resets when app closes)
    public static boolean isSkipShizukuTransient() {
        return skipShizukuTransient;
    }

    // Global method to check if Shizuku should be skipped (persistent or transient)
    public static boolean isSkipShizuku(Context ctx) {
        return isSkipShizukuTransient() || isSkipShizukuPersisted(ctx);
    }

    // Save skip flags
    public static void setSkipShizukuPersisted(Context ctx, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putBoolean("skip_shizuku", value).apply();
    }

    public static void setSkipShizukuTransient(boolean value) {
        skipShizukuTransient = value;
    }

    // Reset transient flag (called when app restarts or CheckPerm closes)
    public static void resetSkipFlags(Context ctx) {
        skipShizukuTransient = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().remove("skip_shizuku").apply();
    }
}
