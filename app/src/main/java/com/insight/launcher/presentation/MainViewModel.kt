package com.insight.launcher.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase
import com.insight.launcher.presentation.model.AppUiModel

class MainViewModel(
    application: Application,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase
) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppUiModel>>()
    val apps: LiveData<List<AppUiModel>> = _apps

    fun fetchApps() {
        val pm = getApplication<Application>().packageManager
        val domainApps = getInstalledAppsUseCase()
        
        val uiApps = domainApps.map {
            AppUiModel(
                label = it.label,
                packageName = it.packageName,
                icon = pm.getApplicationIcon(it.packageName)
            )
        }
        _apps.value = uiApps
    }
}
