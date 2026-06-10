package com.zipflash.mrp.helper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import com.zipflash.mrp.SettingsHelper
import com.zipflash.mrp.models.AppInfo

object AppManager {

    fun loadInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val helper = SettingsHelper(context)
        val showActivities = helper.showActivities
        val showSystem = helper.showSystemApps

        val tempList = mutableListOf<AppInfo>()
        val launcherPackages = mutableSetOf<String>()
        val launcherActivityNames = mutableSetOf<String>()

        if (showActivities) {
            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchers: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            for (ri in launchers) {
                val appInfo = ri.activityInfo.applicationInfo
                val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                if (!showSystem && isSystem) continue

                val appName = ri.loadLabel(pm).toString()
                val icon = ri.loadIcon(pm)
                val packageName = ri.activityInfo.packageName
                val activityName = ri.activityInfo.name

                tempList.add(AppInfo("$appName • $activityName", packageName, activityName, icon))
                launcherPackages.add(packageName)
                launcherActivityNames.add(activityName)
            }
        }

        val flags = PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_DISABLED_COMPONENTS or
            PackageManager.GET_ACTIVITIES

        val packages: List<PackageInfo> = pm.getInstalledPackages(flags)

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            if (!showSystem && isSystem) continue

            val appName = appInfo.loadLabel(pm).toString()
            val icon: Drawable = try {
                appInfo.loadIcon(pm)
            } catch (e: Exception) {
                context.getDrawable(android.R.drawable.sym_def_app_icon)!!
            }
            val packageName = pkg.packageName
            val hasLauncher = showActivities && launcherPackages.contains(packageName)

            if (showActivities) {
                pkg.activities?.forEach { activity ->
                    if (!(hasLauncher && launcherActivityNames.contains(activity.name))) {
                        tempList.add(AppInfo("$appName • ${activity.name}", packageName, activity.name, icon))
                    }
                }

                pkg.services?.forEach { service ->
                    tempList.add(AppInfo("$appName • ${service.name}", packageName, service.name, icon))
                }

                pkg.receivers?.forEach { receiver ->
                    tempList.add(AppInfo("$appName • ${receiver.name}", packageName, receiver.name, icon))
                }

                pkg.providers?.forEach { provider ->
                    tempList.add(AppInfo("$appName • ${provider.name}", packageName, provider.name, icon))
                }
            } else {
                tempList.add(AppInfo(appName, packageName, icon))
            }
        }

        tempList.sortWith(Comparator { a1: AppInfo, a2: AppInfo ->
            a1.name.compareTo(a2.name, ignoreCase = true)
        })

        return tempList
    }
}
