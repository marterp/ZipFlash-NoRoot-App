package com.zipflash.mrp.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;

import com.zipflash.mrp.BuildConfig;
import com.zipflash.mrp.MainActivity;
import com.zipflash.mrp.R;

import rikka.shizuku.Shizuku;
import android.view.LayoutInflater;
import android.widget.CheckBox;

public class CheckPerm extends AppCompatActivity {

    private static final int REQUEST_CODE_SHIZUKU = 1000;
    private static final String CHANNEL_ID = "zipflash_channel";

    private Handler handler;
    private Runnable checkRunnable;

    private TextView tvMessage;
    private Button btnRetry;
    private Button btnShizukuAction;

    private boolean movedToMain = false;
    private boolean skipShizuku = false;
    private static boolean hideWarningUntilClose = false;

    private SharedPreferences prefs;

    // Safe disable for release
    private static final boolean USE_SHIZUKU_PROVIDER = BuildConfig.DEBUG;

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
	new Shizuku.OnRequestPermissionResultListener() {
		@Override
		public void onRequestPermissionResult(int requestCode, int grantResult) {
			if (requestCode == REQUEST_CODE_SHIZUKU) {
				if (grantResult == PackageManager.PERMISSION_GRANTED) {
					showPermissionGranted();
					delayGoToMain();
				} else {
					showMessage("Permission denied. Please try again.",
                                android.R.color.holo_red_dark);
				}
			}
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkperm);

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        btnRetry = (Button) findViewById(R.id.btnGrant);
        btnShizukuAction = (Button) findViewById(R.id.btnShizukuAction);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        getSupportActionBar().setTitle("Permission");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        skipShizuku = prefs.getBoolean("skip_shizuku", false);

        handler = new Handler();
        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        btnRetry.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					updateStatus();
				}
			});

        // Skip path
        if (skipShizuku || hideWarningUntilClose) {
            delayGoToMain();
            return;
        }

        updateStatus();

        // Recheck periodically in debug builds
        if (BuildConfig.DEBUG) {
            checkRunnable = new Runnable() {
                @Override
                public void run() {
                    updateStatus();
                    handler.postDelayed(this, 2000);
                }
            };
            handler.post(checkRunnable);
        }
    }

    private void updateStatus() {
        if (movedToMain) return;

        boolean shizukuInstalled;
        try {
            getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            shizukuInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            shizukuInstalled = false;
        }

        // --- If Shizuku missing ---
        if (!shizukuInstalled) {
            // Check if WRITE_SECURE_SETTINGS granted
            if (CheckPermHelper.hasWriteSecureSettings(this)) {
                showSkipDialog(); // allow limited mode
                return;
            }
            showMessage("Shizuku not installed.\nPlease install from Play Store or continue in limited mode.", android.R.color.holo_red_dark);
            btnShizukuAction.setVisibility(View.GONE);
            if (!BuildConfig.DEBUG) delayGoToMain();
            return;
        }

        // --- If Shizuku not running ---
        if (!Shizuku.pingBinder()) {
            showMessage("Shizuku is not running. Please start Shizuku.", android.R.color.darker_gray);
            btnShizukuAction.setVisibility(View.VISIBLE);
            btnShizukuAction.setText("Open Shizuku");
            btnShizukuAction.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent launchIntent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
						if (launchIntent != null) {
							startActivity(launchIntent);
						} else {
							showMessage("Unable to open Shizuku app.", android.R.color.holo_red_dark);
						}
					}
				});
            return;
        }

        // --- Request permission if needed ---
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            showMessage("Requesting Shizuku permission...", android.R.color.holo_orange_dark);
            btnShizukuAction.setVisibility(View.VISIBLE);
            btnShizukuAction.setText("Grant Permission");
            btnShizukuAction.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
					}
				});
            return;
        }

        // --- Permission granted ---
        showPermissionGranted();
        delayGoToMain();
    }

    private void delayGoToMain() {
        if (movedToMain) return;
        movedToMain = true;

        handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					startActivity(new Intent(CheckPerm.this, MainActivity.class));
					overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
					finish();
				}
			}, 300);
    }

    private void showPermissionGranted() {
        tvMessage.setText("Permission granted on ZipFlash!");
        tvMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        btnShizukuAction.setVisibility(View.GONE);
        showNotification("ZipFlash", "Shizuku permission granted!");
    }

    private void showMessage(String msg, int colorRes) {
        tvMessage.setText(msg);
        tvMessage.setTextColor(getResources().getColor(colorRes));
    }

    private void showNotification(String title, String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID, "ZipFlash Permission", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for ZipFlash permission");
            nm.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle(title)
			.setContentText(message)
			.setSmallIcon(android.R.drawable.ic_dialog_info)
			.setAutoCancel(true)
			.setOnlyAlertOnce(true);
        nm.notify(1, builder.build());
    }

	// --- Skip / Limited mode dialog ---
	private void showSkipDialog() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View dialogView = inflater.inflate(R.layout.dialog_skip, null);

		final Button btnContinue = dialogView.findViewById(R.id.btnContinue);
		final Button btnGrant = dialogView.findViewById(R.id.btnGrant);

		final AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
			.setView(dialogView)
			.setCancelable(true)
			.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(R.drawable.popup_bg);
		}
		dialog.setCanceledOnTouchOutside(true);

		// Continue (Limited Mode)
		btnContinue.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					com.zipflash.mrp.helper.CheckPermHelper.setSkipShizukuTransient(true);
					dialog.dismiss();
					delayGoToMain();
				}
			});

		// Grant Shizuku Permission
		btnGrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					dialog.dismiss();
				}
			});

		dialog.show();
	}
	
    // --- Toolbar Skip menu ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.checkperm_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_skip) {
            showSkipDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
    }

    @Override
    public void onBackPressed() {
        // disable back button
    }
}
