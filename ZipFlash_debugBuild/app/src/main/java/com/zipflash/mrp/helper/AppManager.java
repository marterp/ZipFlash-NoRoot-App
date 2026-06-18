package com.zipflash.mrp.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.*;
import android.graphics.drawable.Drawable;

import java.util.*;

import com.zipflash.mrp.models.AppInfo;
import com.zipflash.mrp.SettingsHelper;

public class AppManager {

    public static List<AppInfo> loadInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        SettingsHelper helper = new SettingsHelper(context);
        boolean showActivities = helper.getShowActivities();
        boolean showSystem = helper.getShowSystemApps();

        List<AppInfo> tempList = new ArrayList<>();
        Set<String> launcherPackages = new HashSet<>();

        // Get launcher activities
        if (showActivities) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> launchers = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : launchers) {
                ApplicationInfo appInfo = ri.activityInfo.applicationInfo;
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (!showSystem && isSystem) continue;

                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                String packageName = ri.activityInfo.packageName;
                String activityName = ri.activityInfo.name;

                tempList.add(new AppInfo(appName + " • " + activityName,
                                         packageName,
                                         activityName,
                                         icon));
                launcherPackages.add(packageName);
            }
        }

        // Flags to get all components
        int flags = PackageManager.GET_SERVICES |
            PackageManager.GET_RECEIVERS |
            PackageManager.GET_PROVIDERS |
            PackageManager.GET_META_DATA |
            PackageManager.GET_DISABLED_COMPONENTS |
            PackageManager.GET_ACTIVITIES;

        List<PackageInfo> packages = pm.getInstalledPackages(flags);

        for (PackageInfo pkg : packages) {
            ApplicationInfo appInfo = pkg.applicationInfo;
            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (!showSystem && isSystem) continue;

            String appName = appInfo.loadLabel(pm).toString();
            Drawable icon;
            try {
                icon = appInfo.loadIcon(pm);
            } catch (Exception e) {
                icon = context.getDrawable(android.R.drawable.sym_def_app_icon);
            }
            String packageName = pkg.packageName;
            boolean hasLauncher = showActivities && launcherPackages.contains(packageName);

            if (showActivities) {
                // Add all activities
                if (pkg.activities != null) {
                    for (ActivityInfo activity : pkg.activities) {
                        // Skip launcher activity already added
                        if (hasLauncher && activity.name.equals(activity.name)) continue;

                        tempList.add(new AppInfo(appName + " • " + activity.name,
                                                 packageName,
                                                 activity.name,
                                                 icon));
                    }
                }

                // Add all services
                if (pkg.services != null) {
                    for (ServiceInfo service : pkg.services) {
                        tempList.add(new AppInfo(appName + " • " + service.name,
                                                 packageName,
                                                 service.name,
                                                 icon));
                    }
                }

                // Add receivers
                if (pkg.receivers != null) {
                    for (ActivityInfo receiver : pkg.receivers) {
                        tempList.add(new AppInfo(appName + " • " + receiver.name,
                                                 packageName,
                                                 receiver.name,
                                                 icon));
                    }
                }

                // Add providers
                if (pkg.providers != null) {
                    for (ProviderInfo provider : pkg.providers) {
                        tempList.add(new AppInfo(appName + " • " + provider.name,
                                                 packageName,
                                                 provider.name,
                                                 icon));
                    }
                }

            } else {
                // If showActivities is false, add only the app itself
                tempList.add(new AppInfo(appName, packageName, icon));
            }
        }

        // Sort alphabetically
        Collections.sort(tempList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a1, AppInfo a2) {
                    return a1.getName().compareToIgnoreCase(a2.getName());
                }
            });

        return tempList;
    }
}
