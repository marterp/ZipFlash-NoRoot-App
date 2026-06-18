package com.zipflash.mrp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

import com.zipflash.mrp.helper.UpdateChecker;
import com.zipflash.mrp.helper.ShellHelper;
import com.zipflash.mrp.helper.AppManager;
import com.zipflash.mrp.helper.FileHelper;
import com.zipflash.mrp.helper.ScriptRunner;
import com.zipflash.mrp.helper.ZipExtractor;
import com.zipflash.mrp.helper.CheckPerm;
import com.zipflash.mrp.helper.PermissionHelper;

import com.zipflash.mrp.models.AppInfo;
import com.zipflash.mrp.ui.AppsAdapter;
import android.view.WindowManager;
import android.content.ActivityNotFoundException;

import android.app.ProgressDialog;
import java.io.File;
import com.zipflash.mrp.helper.CheckPermHelper;
import com.zipflash.mrp.manager.ModuleManager;
import com.zipflash.mrp.helper.ShellHelper;
import com.zipflash.mrp.ui.LimitedModeBanner;



public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE_PICK_ZIP = 2000;

    private TextView tvOutput;
    private Button btnSelectFile, btnRunScript;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private CheckBox cbRevert;

    private Uri selectedZipUri;
    private String workDir;
    private List<AppInfo> allApps = new ArrayList<>();
    private AppsAdapter appsAdapter;
    private SettingsHelper settingsHelper;
	
	private ProgressDialog progressDialog;
	private boolean hasSyncedOnce = false;
	private LimitedModeBanner limitedBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		//limitedBanner = new LimitedModeBanner(this);
		//limitedBanner.updateVisibility();

        // Shizuku permission check
        /*if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, CheckPerm.class));
            finish();
            return;
        }*/

        // App update check
        String currentVersion = "1.0.0";
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        new UpdateChecker(this,
                          "https://raw.githubusercontent.com/marterp/ZipFlash-NoRoot/refs/heads/main/update.json"
                          ).checkForUpdate(currentVersion);

        // UI setup
        tvOutput = findViewById(R.id.tvOutput);
        tvOutput.setMovementMethod(new ScrollingMovementMethod());

        btnSelectFile = findViewById(R.id.btnSelect);
        btnRunScript = findViewById(R.id.btnRun);
        btnRunScript.setEnabled(false);

        cbRevert = findViewById(R.id.cbRevert);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        preloadApps();
        setupToolbar();
        setupNavigationDrawer();
        setupButtons();

        // Storage permission
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermission(this);
        } else {
            createMRPFolder();
        }
        
        settingsHelper = new SettingsHelper(this);
    }

    // ---------------- UI Setup ----------------

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        //toolbar.setBackgroundColor(Color.parseColor("#333333")); // Dark background to match theme

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_drawer_menu); // Custom hamburger icon
            actionBar.setTitle("ZipFlash - No Root");
        }

        // Tint the navigation icon white
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }
    }

    private void setupNavigationDrawer() {
		navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
				@Override
				public boolean onNavigationItemSelected(@NonNull MenuItem item) {
					int id = item.getItemId();
					Intent intent = null;

					if (id == R.id.nav_flash) {
						// Already in MainActivity, close the drawer
						drawerLayout.closeDrawer(GravityCompat.START);
					} else if (id == R.id.nav_shell) {
						intent = new Intent(MainActivity.this, Shell.class);
					} else if (id == R.id.nav_modules_manager) {
						intent = new Intent(MainActivity.this, ModuleManagerActivity.class);
					} else if (id == R.id.nav_modules) {
						intent = new Intent(MainActivity.this, Modules.class);
					} else if (id == R.id.nav_about) {
						intent = new Intent(MainActivity.this, About.class);
					} else if (id == R.id.nav_settings) {
						intent = new Intent(MainActivity.this, SettingsActivity.class);
					} else {
						return false;
					}

					if (intent != null) {
						// Use FLAG_ACTIVITY_REORDER_TO_FRONT to reuse MainActivity when returning
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						intent.putExtra("open_drawer", true); // Pass flag to reopen drawer on return
						startActivity(intent);
					}

					return true;
				}
			});
	}

    private void setupButtons() {
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Try to open ZArchiver first
					Intent zArchiverIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					zArchiverIntent.setPackage("ru.zdevs.zarchiver");
					zArchiverIntent.setType("*/*");
					zArchiverIntent.putExtra(Intent.EXTRA_MIME_TYPES,
											 new String[]{"application/zip", "application/x-sh", "text/plain", "text/x-shellscript"});
					zArchiverIntent.addCategory(Intent.CATEGORY_OPENABLE);
					zArchiverIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					PackageManager pm = getPackageManager();
					if (zArchiverIntent.resolveActivity(pm) != null) {
						try {
							startActivityForResult(zArchiverIntent, REQUEST_CODE_PICK_ZIP);
							return;
						} catch (ActivityNotFoundException e) {
							// If ZArchiver fails, go to fallback
						}
					}

					// Open generic picker
					openGenericFilePicker();
				}

				private void openGenericFilePicker() {
					// Prefer ACTION_GET_CONTENT (can browse more freely than OPEN_DOCUMENT)
					Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
					getContentIntent.setType("*/*");
					getContentIntent.putExtra(Intent.EXTRA_MIME_TYPES,
											  new String[]{"application/zip", "application/x-sh", "text/plain", "text/x-shellscript"});
					getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);
					getContentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					try {
						startActivityForResult(Intent.createChooser(getContentIntent,
																	"Select ZIP, SH, or Text File"), REQUEST_CODE_PICK_ZIP);
					} catch (ActivityNotFoundException e) {
						// Fallback to ACTION_OPEN_DOCUMENT
						Intent openDocIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
						openDocIntent.setType("*/*");
						openDocIntent.putExtra(Intent.EXTRA_MIME_TYPES,
											   new String[]{"application/zip", "application/x-sh", "text/plain", "text/x-shellscript"});
						openDocIntent.addCategory(Intent.CATEGORY_OPENABLE);
						openDocIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						try {
							startActivityForResult(Intent.createChooser(openDocIntent,
																		"Select ZIP, SH, or Text File"), REQUEST_CODE_PICK_ZIP);
						} catch (ActivityNotFoundException e2) {
							Toast.makeText(getApplicationContext(),
										   "No file picker found to select file", Toast.LENGTH_SHORT).show();
						}
					}
				}
			});

        btnRunScript.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedZipUri == null) return;

                    final String fileName = FileHelper.getFileName(MainActivity.this, selectedZipUri);

                    new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (fileName.endsWith(".zip")) {
                                        // ZIP files -> extract and run run.sh or revert.sh
                                        workDir = extractZipToMRP(selectedZipUri);

                                        runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tvOutput.append("[✓] ZIP Extracted\n");
                                                }
                                            });

                                        final String scriptToRun = cbRevert.isChecked()
                                            ? workDir + "/revert.sh"
                                            : workDir + "/run.sh";

                                        // ✅ Pass context to ShellHelper
                                        ShellHelper.runShellScript(MainActivity.this, scriptToRun, tvOutput,
                                            new ShellHelper.OnScriptFinishedListener() {
                                                @Override
                                                public void onFinished() {
                                                    runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                showSuccessDialog();
                                                            }
                                                        });
                                                }

                                                @Override
                                                public void onError(final String error) {
                                                    runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                tvOutput.append("[!] Error: " + error + "\n");
                                                            }
                                                        });
                                                }
                                            });

                                    } else {
                                        // .sh or any executable (if toggle is ON in settings)
                                        ScriptRunner.runSingleSh(MainActivity.this, selectedZipUri, tvOutput,
                                            new ScriptRunner.OnScriptFinishedListener() {
                                                @Override
                                                public void onFinished() {
                                                    runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                showSuccessDialog();
                                                            }
                                                        });
                                                }

                                                @Override
                                                public void onError(final String error) {
                                                    runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                tvOutput.append("[!] Error: " + error + "\n");
                                                            }
                                                        });
                                                }
                                            });
                                    }

                                } catch (final Exception e) {
                                    runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tvOutput.append("[!] Error: " + e.getMessage() + "\n");
                                            }
                                        });
                                }
                            }
                        }).start();
                }
            });
    }

    private void preloadApps() {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<AppInfo> tempList = AppManager.loadInstalledApps(MainActivity.this);
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                allApps.clear();
                                allApps.addAll(tempList);
                                if (appsAdapter != null) {
                                    appsAdapter.updateData(allApps);
                                }
                            }
                        });
                }
            }).start();
    }

    // ---------------- File Handling ----------------

    private void runSingleSh(Uri uri) {
        // ScriptRunner.runSingleSh now expects an optional listener as the last arg; pass null here
        ScriptRunner.runSingleSh(this, uri, tvOutput, null);
    }

    private String extractZipToMRP(Uri uri) throws IOException, InterruptedException {
        return ZipExtractor.extractToModules(this, uri, tvOutput);
    }

    // ---------------- Permissions ----------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionHelper.STORAGE_PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                tvOutput.append("[!] Storage permission denied.\n");
                btnSelectFile.setEnabled(false);
                btnRunScript.setEnabled(false);
            } else {
                createMRPFolder();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void createMRPFolder() {
        // ShellHelper.runShellCommand signature requires a listener parameter; pass null when you don't need callbacks
        ShellHelper.runShellCommand("mkdir -p /data/local/tmp/MRP", tvOutput, null);
    }

    // ---------------- Other ----------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_ZIP && resultCode == RESULT_OK && data != null) {
            selectedZipUri = data.getData();
            final String fileName = FileHelper.getFileName(this, selectedZipUri);

            // Load "Run Any File" toggle
            final SettingsHelper settings = new SettingsHelper(this);
            final boolean anyFileMode = settings.isAnyFileMode();

            tvOutput.setText(""); // clear output

            if (fileName == null) {
                tvOutput.append("[!] Invalid file.\n");
                cbRevert.setVisibility(View.GONE);
                btnRunScript.setEnabled(false);
                return;
            }

            if (fileName.endsWith(".zip")) {
                // ZIP files
                cbRevert.setVisibility(View.VISIBLE);
                tvOutput.append("[✓] File selected: " + fileName + "\n");
                tvOutput.append("[✓] ZIP selected. Ready to run.\n");
                btnRunScript.setEnabled(true);

            } else if (fileName.endsWith(".sh")) {
                // SH scripts
                cbRevert.setVisibility(View.GONE);
                tvOutput.append("[✓] File selected: " + fileName + "\n");
                tvOutput.append("[✓] SH script selected. Ready to run.\n");
                btnRunScript.setEnabled(true);

            } else if (anyFileMode) {
                // Any executable allowed
                cbRevert.setVisibility(View.GONE);
                tvOutput.append("[✓] File selected: " + fileName + "\n");
                tvOutput.append("[✓] Ready to run\n");
                btnRunScript.setEnabled(true);

            } else {
                // Unsupported file
                cbRevert.setVisibility(View.GONE);
                tvOutput.append("[!] Unsupported file type.\n");
                btnRunScript.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null)
                menu.getItem(i).getIcon().setTint(Color.WHITE);
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_apps) {
            showAppPickerPopup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_success, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnOk = view.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        dialog.show();
    }

    private void showAppPickerPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppPickerDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_app_picker, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            // Keep default dialog background but make it transparent-ish
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Add dim amount for background transparency control
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.6f; // 0.0 = no dim, 1.0 = fully dim
            dialog.getWindow().setAttributes(lp);

            // Prevent initial focus issue
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        EditText searchApp = view.findViewById(R.id.searchApp);
        RecyclerView recyclerApps = view.findViewById(R.id.recyclerApps);
        Button btnClose = view.findViewById(R.id.btnClose);

        if (allApps == null || allApps.isEmpty()) {
            Toast.makeText(this, "No apps available", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        appsAdapter = new AppsAdapter(this, new ArrayList<>(allApps));
        recyclerApps.setLayoutManager(new LinearLayoutManager(this));
        recyclerApps.setAdapter(appsAdapter);

        searchApp.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No-op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (appsAdapter != null) {
                        appsAdapter.filter(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // No-op
                }
            });

        btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        dialog.show();
        // Clear focusable flag after showing to allow interaction (e.g., typing in search)
        if (dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

	private void attemptSyncModulesIfNeeded() {
		if (CheckPermHelper.hasShizukuPermission() && !hasSyncedOnce) {
			hasSyncedOnce = true;
			syncModulesWithShizuku();
		}
	}

	private void syncModulesWithShizuku() {
		new Thread(new Runnable() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showProgress("Synchronizing modules...");
							}
						});

					try {
						ModuleManager mm = new ModuleManager(MainActivity.this);
						File[] modules = mm.listModules();
						if (modules != null) {
							for (int i = 0; i < modules.length; i++) {
								File m = modules[i];
								if (m.isFile() && m.getName().toLowerCase().endsWith(".zip")) {
									String src = m.getAbsolutePath().replace("'", "'\\''");
									String cmd = "cp -f '" + src + "' /data/local/tmp/modules/";
									ShellHelper.runPrivilegedCommand(cmd);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									hideProgress();
									if (limitedBanner != null) limitedBanner.updateVisibility();
								}
							});
					}
				}
			}).start();
	}

	private void showProgress(String msg) {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
		}
		progressDialog.setMessage(msg);
		progressDialog.show();
	}

	private void hideProgress() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}
	
	
   @Override
    public boolean onSupportNavigateUp() {
        drawerLayout.openDrawer(GravityCompat.START);
        return true;
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        /*if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, CheckPerm.class));
            finish();
            return;
        }*/
	
        // Reload apps list if setting changed
        preloadApps();
		super.onResume();
		if (limitedBanner != null) limitedBanner.updateVisibility();
		attemptSyncModulesIfNeeded();
	
    }
	
		
	
	
		
}
