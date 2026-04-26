package com.insight.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.insight.launcher.domain.model.AppModel
import com.insight.launcher.domain.repository.AppRepository

class AppRepositoryImpl(private val context: Context) : AppRepository {
    override fun getInstalledApps(): List<AppModel> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        
        return resolveInfos.mapNotNull { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            if (appInfo.packageName == context.packageName || !appInfo.enabled) null
            else {
                AppModel(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName
                )
            }
        }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun canUninstallApp(packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
