package com.zipflash.mrp.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static final int STORAGE_PERMISSION_CODE = 100;

    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10–12
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 9 and below
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+                 
            ActivityCompat.requestPermissions(activity,
                                              new String[]{
                                                  Manifest.permission.READ_MEDIA_IMAGES,
                                                  Manifest.permission.READ_MEDIA_VIDEO,
                                                  Manifest.permission.READ_MEDIA_AUDIO
                                              },
                                              STORAGE_PERMISSION_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10–12
            ActivityCompat.requestPermissions(activity,
                                              new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                              STORAGE_PERMISSION_CODE);
        } else {
            // Android 9 and below
            ActivityCompat.requestPermissions(activity,
                                              new String[]{
                                                  Manifest.permission.READ_EXTERNAL_STORAGE,
                                                  Manifest.permission.WRITE_EXTERNAL_STORAGE
                                              },
                                              STORAGE_PERMISSION_CODE);
        }
    }
}
