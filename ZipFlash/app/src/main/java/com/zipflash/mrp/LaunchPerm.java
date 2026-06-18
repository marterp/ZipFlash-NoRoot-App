package com.zipflash.mrp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.content.pm.ApplicationInfo;
import java.util.List;
import com.zipflash.mrp.helper.PermissionHelper;

public class LaunchPerm extends BaseActivity {

    private static final int REQUEST_CODE_STORAGE = 2001;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 2002;
    private static final int REQUEST_CODE_INSTALL_APK = 2003;

    private TextView tvMessage;
    private Button btnGrant;
    private int currentStep = 0; // 0=storage,1=write_settings,2=install_apk,3=list_apps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_perm);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Permissions");
        }

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        btnGrant = (Button) findViewById(R.id.btnGrant);

        checkNextPermission();
    }

    private void checkNextPermission() {
        switch (currentStep) {
            case 0: // STORAGE
                if (!PermissionHelper.hasStoragePermission(this)) {
                    setPermissionStep(
                        "Storage permission is required for ZipFlash to function properly.",
                        "Continue",
                        Color.WHITE,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    showPermissionRequest(
                                        "Storage permission is required",
                                        "Grant Storage Permission",
                                        REQUEST_CODE_STORAGE,
                                        new String[]{
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO,
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        }
                                    );
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    showPermissionRequest(
                                        "Storage permission is required",
                                        "Grant Storage Permission",
                                        REQUEST_CODE_STORAGE,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                                    );
                                } else {
                                    showPermissionRequest(
                                        "Storage permission is required",
                                        "Grant Storage Permission",
                                        REQUEST_CODE_STORAGE,
                                        new String[]{
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        }
                                    );
                                }
                            }
                        }
                    );
                } else {
                    showPermissionGranted("Storage permission granted!");
                }
                break;

            case 1: // WRITE_SETTINGS
                if (!Settings.System.canWrite(this)) {
                    setPermissionStep(
                        "System settings permission is required.",
                        "Grant System Settings",
                        Color.RED,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityForResult(
                                    new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                               Uri.parse("package:" + getPackageName())),
                                    REQUEST_CODE_WRITE_SETTINGS
                                );
                            }
                        }
                    );
                } else {
                    showPermissionGranted("System settings permission granted!");
                }
                break;

            case 2: // INSTALL_APK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !getPackageManager().canRequestPackageInstalls()) {
                    setPermissionStep(
                        "Permission to install apps is required.",
                        "Allow Install Apps",
                        Color.RED,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityForResult(
                                    new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                               Uri.parse("package:" + getPackageName())),
                                    REQUEST_CODE_INSTALL_APK
                                );
                            }
                        }
                    );
                } else {
                    showPermissionGranted("Install apps permission granted!");
                }
                break;

            case 3: // LIST_INSTALLED_APPS
                setPermissionStep(
                    "Grant access to list apps",
                    "Continue",
                    Color.RED,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PackageManager pm = getPackageManager();
                            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                            showPermissionGranted("List apps permission granted!");
                        }
                    }
                );
                break;

            default: // AUTO GO TO MAIN
                markPermissionsDone();
                goToMain();
                break;
        }
    }

    /** Standardized method to show granted message in green and move next */
    private void showPermissionGranted(String message) {
        tvMessage.setText(message);
        tvMessage.setTextColor(Color.GREEN);
        btnGrant.setText("Continue");
        btnGrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					currentStep++;
					checkNextPermission();
				}
			});
    }

    /** Helper method to standardize message, button text, color, and click listener */
    private void setPermissionStep(String message, String buttonText, int color, View.OnClickListener listener) {
        tvMessage.setText(message);
        tvMessage.setTextColor(color);
        btnGrant.setText(buttonText);
        btnGrant.setOnClickListener(listener);
    }

    private void showPermissionRequest(String message, String buttonText,
                                       final int requestCode, final String[] permissions) {
        tvMessage.setText(message);
        tvMessage.setTextColor(Color.RED);
        btnGrant.setText(buttonText);
        btnGrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ActivityCompat.requestPermissions(LaunchPerm.this, permissions, requestCode);
				}
			});
    }

    private void markPermissionsDone() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit()
            .putBoolean("launch_perm_done", true)
            .putBoolean("welcome_done", true) // also mark welcome done here safely
            .apply();
    }

    private void goToMain() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean launchPermDone = prefs.getBoolean("launch_perm_done", false);
        boolean welcomeDone = prefs.getBoolean("welcome_done", false);

        if (launchPermDone && welcomeDone) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                currentStep++;
                checkNextPermission();
            } else {
                tvMessage.setText("Storage permission is required to continue.");
                tvMessage.setTextColor(Color.RED);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_INSTALL_APK) {
            showPermissionGranted("Install apps permission granted");
        } else {
            checkNextPermission(); // keep existing behavior
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNextPermission(); // refresh when coming back to app
    }

    @Override
    public void onBackPressed() {
        // Disable back button
    }
}
