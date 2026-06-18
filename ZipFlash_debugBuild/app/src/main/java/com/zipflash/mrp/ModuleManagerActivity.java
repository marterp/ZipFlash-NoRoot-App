package com.zipflash.mrp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

import com.zipflash.mrp.helper.CheckPermHelper;
import com.zipflash.mrp.manager.ModuleManager;
import com.zipflash.mrp.ui.LimitedModeBanner;

public class ModuleManagerActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ModuleManagerAdapter adapter;
    private List<File> moduleList;
    private TextView emptyView;
    private LimitedModeBanner limitedBanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_manager);

        // ---- Toolbar Setup ----
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Module Manager");
        }

        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }

        // ---- RecyclerView ----
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyView = findViewById(R.id.emptyView);

        // ---- Limited Mode Banner ----
        //limitedBanner = new LimitedModeBanner(this);
        //limitedBanner.updateVisibility();

        // ---- Load Modules ----
        if (CheckPermHelper.isSkipShizuku(this) || !CheckPermHelper.hasShizukuPermission()) {
            // Limited mode: load local app modules
            moduleList = loadLocalModules();
        } else {
            // Full mode: load Shizuku modules
            moduleList = loadShizukuModules();
        }

        adapter = new ModuleManagerAdapter(this, moduleList, emptyView);
        recyclerView.setAdapter(adapter);
    }

    // ---- Inflate Toolbar Menu ----
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_module_manager, menu);
        return true;
    }

    // ---- Menu Actions ----
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_info) {
            showInfoDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---- Info Dialog ----
    private void showInfoDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_module_warning, null);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
			.setView(dialogView)
			.setCancelable(true)
			.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnOk.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

        dialog.show();
    }

    // ---- Load Shizuku Modules ----
    private List<File> loadShizukuModules() {
        List<File> modules = new ArrayList<>();
        File modulesDir = new File("/data/local/tmp/modules");

        try {
            if (!modulesDir.exists()) {
                ShizukuRemoteProcess mkdir = Shizuku.newProcess(
					new String[]{"mkdir", "-p", modulesDir.getAbsolutePath()},
					null, null);
                mkdir.waitFor();
                mkdir.destroy();
            }

            if (modulesDir.isDirectory()) {
                File[] files = modulesDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            modules.add(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this,
						   "Error loading Shizuku modules: " + e.getMessage(),
						   Toast.LENGTH_LONG).show();
        }

        return modules;
    }

    // ---- Load Local Modules ----
    private List<File> loadLocalModules() {
        ModuleManager mm = new ModuleManager(this);
        File dir = mm.getModulesDir();
        List<File> modules = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".zip")) {
                    modules.add(f);
                }
            }
        }
        return modules;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (limitedBanner != null) limitedBanner.updateVisibility();
    }
}
