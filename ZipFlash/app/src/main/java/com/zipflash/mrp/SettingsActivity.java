package com.zipflash.mrp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.CompoundButton;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatDelegate;
import android.graphics.Color;
import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;
import com.zipflash.mrp.helper.CheckPerm;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.content.Context;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class SettingsActivity extends BaseActivity {

    private SettingsHelper settingsHelper;

    // Cached view references
    private SwitchCompat switchAnyFile;
    private SwitchCompat switchShowActivities;
    private SwitchCompat switchShowSystem;
    private SwitchCompat switchOptimize;

    private RadioGroup radioFont;
    private RadioGroup radioTheme; // Optional in layout; guarded by null checks

    private Spinner spinnerLanguage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		setupToolbar();
		initializeSettings();
		cacheViewReferences();

		setupLanguageSpinner();   
		setupFontRadioGroup();
		setupThemeRadioGroup();
		setupSwitches();
		setupButtons();
	}

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Settings");
        }

        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }
    }

    private void initializeSettings() {
        settingsHelper = new SettingsHelper(getApplicationContext());
    }

    private void cacheViewReferences() {
        switchAnyFile = (SwitchCompat) findViewById(R.id.switch_any_file);
        switchShowActivities = (SwitchCompat) findViewById(R.id.switch_show_activities);
        switchShowSystem = (SwitchCompat) findViewById(R.id.switch_show_system);
        switchOptimize = (SwitchCompat) findViewById(R.id.optimize_switch);

        radioFont = (RadioGroup) findViewById(R.id.radio_font);
        radioTheme = (RadioGroup) findViewById(R.id.radio_theme); // may be null if not in layout

        spinnerLanguage = (Spinner) findViewById(R.id.spinner_language); // may be null if not added yet
    }

    // -------- Language Spinner --------
    private void setupLanguageSpinner() {
		spinnerLanguage = (Spinner) findViewById(R.id.spinner_language);
		if (spinnerLanguage == null) return;

		final String[] labels = new String[] {
			"System default", "English", 
			//"Spanish", "Hindi", "Arabic","Portuguese", "Russian", "Turkish", "Vietnamese", "Indonesian", "Chinese"
		};
		final String[] codes = new String[] {
			"system", "en", //"es", "hi", "ar", "pt", "ru", "tr", "vi", "id", "zh"
		};

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, labels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerLanguage.setAdapter(adapter);

		String current = settingsHelper.getLanguage();
		int sel = 0;
		for (int i = 0; i < codes.length; i++) {
			if (codes[i].equals(current)) { sel = i; break; }
		}
		spinnerLanguage.setSelection(sel, false);

		spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				boolean initialized = false;

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (!initialized) { initialized = true; return; }
					String code = codes[position];
					settingsHelper.setLanguage(code);
					recreate(); // BaseActivity will apply the locale on next attachBaseContext
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) { }
			});
	}

    // -------- Font Radios (Enum) --------
    private void setupFontRadioGroup() {
        if (radioFont == null) return;

        SettingsHelper.FontType font = settingsHelper.getFont();
        setFontRadioButtonChecked(font);

        radioFont.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					SettingsHelper.FontType selectedFont = getFontTypeFromRadioId(checkedId);
					settingsHelper.setFont(selectedFont);
					applySelectedFontToView(getWindow().getDecorView());
					Toast.makeText(SettingsActivity.this, "Font changed", Toast.LENGTH_SHORT).show();
				}
			});
    }

    private SettingsHelper.FontType getFontTypeFromRadioId(int radioId) {
        if (radioId == R.id.radio_font_sans) {
            return SettingsHelper.FontType.SANS;
        } else if (radioId == R.id.radio_font_serif) {
            return SettingsHelper.FontType.SERIF;
        } else {
            return SettingsHelper.FontType.MONOSPACE;
        }
    }

    private void setFontRadioButtonChecked(SettingsHelper.FontType font) {
        int radioId;
        switch (font) {
            case SANS:
                radioId = R.id.radio_font_sans; break;
            case SERIF:
                radioId = R.id.radio_font_serif; break;
            default:
                radioId = R.id.radio_font_monospace; break;
        }
        RadioButton rb = (RadioButton) findViewById(radioId);
        if (rb != null) rb.setChecked(true);
    }

    // -------- Theme Radios (guarded) --------
    private void setupThemeRadioGroup() {
        if (radioTheme == null) return; // Skip if not present in XML

        SettingsHelper.ThemeMode currentTheme = settingsHelper.getThemeMode();
        setThemeRadioButtonChecked(currentTheme);

        radioTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					SettingsHelper.ThemeMode selectedTheme = getThemeModeFromRadioId(checkedId);
					settingsHelper.setThemeMode(selectedTheme);
					applyThemeMode(selectedTheme);
					Toast.makeText(SettingsActivity.this, "Theme changed", Toast.LENGTH_SHORT).show();
				}
			});
    }

    private SettingsHelper.ThemeMode getThemeModeFromRadioId(int radioId) {
        if (radioId == R.id.radio_theme_light) {
            return SettingsHelper.ThemeMode.LIGHT;
        } else if (radioId == R.id.radio_theme_dark) {
            return SettingsHelper.ThemeMode.DARK;
        } else {
            return SettingsHelper.ThemeMode.FOLLOW_SYSTEM;
        }
    }

    private void setThemeRadioButtonChecked(SettingsHelper.ThemeMode theme) {
        if (radioTheme == null) return;
        int radioId;
        switch (theme) {
            case LIGHT: radioId = R.id.radio_theme_light; break;
            case DARK:  radioId = R.id.radio_theme_dark; break;
            default:    radioId = R.id.radio_theme_system; break;
        }
        RadioButton btn = (RadioButton) findViewById(radioId);
        if (btn != null) btn.setChecked(true);
    }

    private void applyThemeMode(SettingsHelper.ThemeMode mode) {
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

    // -------- Switches --------
    private void setupSwitches() {
        setupAnyFileSwitch();
        setupShowActivitiesSwitch();
        setupShowSystemAppsSwitch();
        setupOptimizeScriptSwitch();
    }

    private void setupAnyFileSwitch() {
        if (switchAnyFile == null) return;
        switchAnyFile.setChecked(settingsHelper.isAnyFileMode());
        switchAnyFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settingsHelper.setAnyFileMode(isChecked);
					//Toast.makeText(SettingsActivity.this,"Any file mode: " + (isChecked ? "ON" : "OFF"),Toast.LENGTH_SHORT).show();
				}
			});
    }

    private void setupShowActivitiesSwitch() {
        if (switchShowActivities == null) return;
        switchShowActivities.setChecked(settingsHelper.getShowActivities());
        switchShowActivities.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settingsHelper.setShowActivities(isChecked);
				}
			});
    }

    private void setupShowSystemAppsSwitch() {
        if (switchShowSystem == null) return;
        switchShowSystem.setChecked(settingsHelper.getShowSystemApps());
        switchShowSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settingsHelper.setShowSystemApps(isChecked);
				}
			});
    }

    private void setupOptimizeScriptSwitch() {
        if (switchOptimize == null) return;
        switchOptimize.setChecked(settingsHelper.isOptimizeScriptEnabled());
        switchOptimize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settingsHelper.setOptimizeScriptEnabled(isChecked);
					if (isChecked) {
						//showRestartNotification("Optimize Script","Some optimizations may require app restart to take full effect.");
					}
				}
			});
    }

    // -------- Buttons --------
    private void setupButtons() {
        setupResetButton();
        setupDevOptionsButton();
        setupClearCacheButton();
        setupGrantShizukuButton();
        setupShowIntroButton(); // optional, guarded by null
    }

    private void setupResetButton() {
        Button btnReset = (Button) findViewById(R.id.btn_reset_defaults);
        if (btnReset == null) return;
        btnReset.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showResetDialog();
				}
			});
    }

    private void setupDevOptionsButton() {
        Button btnDevOptions = (Button) findViewById(R.id.btn_dev_options);
        if (btnDevOptions == null) return;
        btnDevOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
					} catch (Exception e) {
						startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					}
				}
			});
    }

    private void setupClearCacheButton() {
        final Button btnClearCache = (Button) findViewById(R.id.btn_clear_cache);
        if (btnClearCache == null) return;
        btnClearCache.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					v.setEnabled(false);
					clearAppCache();
					Toast.makeText(SettingsActivity.this, "Cache cleared successfully.", Toast.LENGTH_SHORT).show();
					v.postDelayed(new Runnable() {
							@Override
							public void run() {
								v.setEnabled(true);
							}
						}, 2000);
				}
			});
    }

    private void setupGrantShizukuButton() {
        Button btnGrantShizuku = (Button) findViewById(R.id.btn_grant_shizuku);
        if (btnGrantShizuku == null) return;

        boolean isLimitedMode = false;
        try {
            if (!Shizuku.pingBinder() ||
				Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                isLimitedMode = true;
            }
        } catch (Throwable e) {
            isLimitedMode = true;
        }

        if (isLimitedMode) {
            btnGrantShizuku.setVisibility(View.VISIBLE);
            btnGrantShizuku.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(SettingsActivity.this, CheckPerm.class));
					}
				});
        } else {
            btnGrantShizuku.setVisibility(View.GONE);
        }
    }

    private void setupShowIntroButton() {
        Button btnShowIntro = (Button) findViewById(R.id.btn_show_intro);
        if (btnShowIntro == null) return;
        btnShowIntro.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Replace with your intro activity if needed
					// startActivity(new Intent(SettingsActivity.this, LaunchPerm.class));
					Toast.makeText(SettingsActivity.this, "Intro not implemented", Toast.LENGTH_SHORT).show();
				}
			});
    }

    // -------- Dialog helpers --------
    private void showResetDialog() {
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_reset, null);
        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
			.setView(customView)
			.setCancelable(true)
			.create();

        View yes = customView.findViewById(R.id.btnYes);
        View no  = customView.findViewById(R.id.btnNo);

        if (yes != null) {
            yes.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						settingsHelper.resetDefaults();
						updateAllViews();
						Toast.makeText(SettingsActivity.this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
						dialog.dismiss();
					}
				});
        }

        if (no != null) {
            no.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
        }

        dialog.show();
    }

    private void showRestartNotification(String feature, String message) {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
			.setTitle(feature)
			.setMessage(message)
			.setPositiveButton("OK", null)
			.show();
    }

    // -------- Helpers --------
    private void updateAllViews() {
        setFontRadioButtonChecked(settingsHelper.getFont());
        setThemeRadioButtonChecked(settingsHelper.getThemeMode());
        updateSwitches();
        applySelectedFontToView(getWindow().getDecorView());
        // Language Spinner will re-bind on recreate; no action here
    }

    private void updateSwitches() {
        if (switchAnyFile != null) switchAnyFile.setChecked(settingsHelper.isAnyFileMode());
        if (switchShowActivities != null) switchShowActivities.setChecked(settingsHelper.getShowActivities());
        if (switchShowSystem != null) switchShowSystem.setChecked(settingsHelper.getShowSystemApps());
        if (switchOptimize != null) switchOptimize.setChecked(settingsHelper.isOptimizeScriptEnabled());
    }
}
