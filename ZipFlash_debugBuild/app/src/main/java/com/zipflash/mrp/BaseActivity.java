package com.zipflash.mrp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.zipflash.mrp.helper.CheckPermHelper;

import java.io.File;
import java.util.Locale;

import rikka.shizuku.Shizuku;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * Base activity that handles:
 * - Locale application before view inflation
 * - Theme application before view inflation
 * - Font application after setContentView
 * - Centralized Shizuku / limited-mode permission checking
 */
public class BaseActivity extends AppCompatActivity {

    protected SettingsHelper settings;

    // --- Restart protection flags ---
    private static boolean isRestarting = false;
    private static boolean lastShizukuAvailable = false;
    private static boolean lastPermissionGranted = false;

    // --- Shizuku change listeners ---
    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
	new Shizuku.OnBinderReceivedListener() {
		@Override
		public void onBinderReceived() {
			handleModeChange();
		}
	};

    private final Shizuku.OnBinderDeadListener binderDeadListener =
	new Shizuku.OnBinderDeadListener() {
		@Override
		public void onBinderDead() {
			handleModeChange();
		}
	};

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
	new Shizuku.OnRequestPermissionResultListener() {
		@Override
		public void onRequestPermissionResult(int requestCode, int grantResult) {
			handleModeChange();
		}
	};

    // Ensure chosen language is applied before onCreate -> setContentView
    @Override
    protected void attachBaseContext(Context newBase) {
        String code = newBase.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
			.getString("language", "system");
        Context wrapped = applyLocale(newBase, code);
        super.attachBaseContext(wrapped);
    }

    private static Context applyLocale(Context base, String code) {
        if (code == null || "system".equals(code)) return base;
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Resources res = base.getResources();
        Configuration cfg = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 24) {
            cfg.setLocale(locale);
            return base.createConfigurationContext(cfg);
        } else {
            cfg.locale = locale;
            res.updateConfiguration(cfg, res.getDisplayMetrics());
            return base;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = new SettingsHelper(this);
        applyStoredTheme();
        super.onCreate(savedInstanceState);

        // Initial Shizuku state snapshot
        try {
            lastShizukuAvailable = Shizuku.pingBinder();
            lastPermissionGranted = lastShizukuAvailable &&
				(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
        } catch (Throwable e) {
            lastShizukuAvailable = false;
            lastPermissionGranted = false;
        }

        // Centralized Shizuku permission flow
        if (!CheckPermHelper.isSkipShizuku(this)) {
            try {
                if (!lastShizukuAvailable || !lastPermissionGranted) {
                    Intent i = new Intent(this, com.zipflash.mrp.helper.CheckPerm.class);
                    startActivity(i);
                    finish();
                    return;
                }
            } catch (Throwable e) {
                showLimitedModeDialog(false);
                CheckPermHelper.setSkipShizukuTransient(true);
            }
        } else {
            showLimitedModeDialog(true);
        }

        // Register listeners
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener);
            Shizuku.addBinderDeadListener(binderDeadListener);
            Shizuku.addRequestPermissionResultListener(permissionResultListener);
        } catch (Throwable ignored) { }
    }

    protected void applyStoredTheme() {
        SettingsHelper.ThemeMode mode = settings.getThemeMode();
        switch (mode) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case FOLLOW_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // No onPostCreate attachAdBanner() override anymore

    /** Still used by limited mode dialog to check connection if needed elsewhere */
    protected boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } catch (Throwable e) {
            return false;
        }
    }

    /** Detect Shizuku state changes and auto-restart once granted */
    private void handleModeChange() {
        boolean shizukuAvailable;
        boolean permissionGranted;
        try {
            shizukuAvailable = Shizuku.pingBinder();
            permissionGranted = shizukuAvailable &&
				(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
        } catch (Throwable e) {
            shizukuAvailable = false;
            permissionGranted = false;
        }

        if (!isRestarting &&
			shizukuAvailable && permissionGranted &&
			(!lastShizukuAvailable || !lastPermissionGranted) &&
			!getClass().getSimpleName().equals("CheckPerm")) {

            isRestarting = true;
            restartApp();
        } else if ((lastShizukuAvailable && !shizukuAvailable) ||
				   (lastPermissionGranted && !permissionGranted)) {
            showLimitedModeDialog(false);
        }

        lastShizukuAvailable = shizukuAvailable;
        lastPermissionGranted = permissionGranted;
    }

    /** Cleanly restart the app */
    private void restartApp() {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						Intent intent = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
						if (intent != null) {
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
											| Intent.FLAG_ACTIVITY_NEW_TASK
											| Intent.FLAG_ACTIVITY_CLEAR_TASK);
							startActivity(intent);
							finishAffinity();
						}
					} catch (Throwable ignored) { }
				}
			});
    }

    /** Show Limited Mode dialog if Shizuku unavailable */
    private void showLimitedModeDialog(final boolean skippedManually) {
        if (CheckPermHelper.getHideLimitedWarning()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_limited_mode, null);

        final CheckBox cbHide = dialogView.findViewById(R.id.cbHideWarning);
        final TextView tvMsg = dialogView.findViewById(R.id.tvLimitedMessage);
        Button btnGrant = dialogView.findViewById(R.id.btnGrant);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        if (skippedManually) {
            tvMsg.setText("Permission is currently skipped.  \nSome features are disabled until Shizuku permission is granted.");
        } else {
            tvMsg.setText("Shizuku is unavailable or not running. \nCertain module features require Shizuku to function properly.");
        }

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
			.setView(dialogView)
			.setCancelable(false)
			.create();

        btnGrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(BaseActivity.this, com.zipflash.mrp.helper.CheckPerm.class);
					startActivity(i);
					finish();
					dialog.dismiss();
				}
			});

        btnClose.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (cbHide.isChecked()) {
						CheckPermHelper.setHideLimitedWarning(true);
					}
					dialog.dismiss();
				}
			});

        dialog.show();
    }

    /** Optional cache clearing used by explicit UI actions; keep light in lifecycle */
    protected void clearAppCache() {
        try {
            // Only clear general cache; ad WebView has been removed
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDir(cacheDir);
            }

            File extCache = getExternalCacheDir();
            if (extCache != null && extCache.isDirectory()) {
                deleteDir(extCache);
            }

            try {
                getApplicationContext().deleteDatabase("webview.db");
                getApplicationContext().deleteDatabase("webviewCache.db");
            } catch (Throwable ignored) { }

        } catch (Throwable ignored) { }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) return false;
                }
            }
        }
        return dir != null && dir.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
            Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        } catch (Throwable ignored) { }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applySelectedFontToView(getWindow().getDecorView());
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applySelectedFontToView(getWindow().getDecorView());
    }

    /** Apply font recursively to all text views using enum */
    protected void applySelectedFontToView(View root) {
        if (root == null) return;

        SettingsHelper.FontType fontType = settings.getFont();
        Typeface tf;
        switch (fontType) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;
            case SERIF:
                tf = Typeface.SERIF;
                break;
            case MONOSPACE:
            default:
                tf = Typeface.MONOSPACE;
                break;
        }
        applyTypefaceRecursively(root, tf);
    }

    private void applyTypefaceRecursively(View v, Typeface tf) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyTypefaceRecursively(vg.getChildAt(i), tf);
            }
        } else if (v instanceof TextView) {
            ((TextView) v).setTypeface(tf);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
