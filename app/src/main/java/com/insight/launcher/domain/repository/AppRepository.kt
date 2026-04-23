package com.insight.launcher.domain.repository

import com.insight.launcher.domain.model.AppModel

interface AppRepository {
    fun getInstalledApps(): List<AppModel>
    fun isAppInstalled(packageName: String): Boolean
    fun canUninstallApp(packageName: String): Boolean
}
