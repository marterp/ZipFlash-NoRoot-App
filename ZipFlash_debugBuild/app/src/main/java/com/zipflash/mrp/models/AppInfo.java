package com.zipflash.mrp.models;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String name;
    private String packageName;
    private String activityName;   // null = app entry, not null = activity entry
    private Drawable icon;

    public AppInfo(String name, String packageName, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
        this.activityName = null;
    }

    public AppInfo(String name, String packageName, String activityName, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.activityName = activityName;
        this.icon = icon;
    }

    public String getName() { return name; }
    public String getPackageName() { return packageName; }
    public String getActivityName() { return activityName; }
    public Drawable getIcon() { return icon; }
    public boolean isActivity() { return activityName != null; }
}
