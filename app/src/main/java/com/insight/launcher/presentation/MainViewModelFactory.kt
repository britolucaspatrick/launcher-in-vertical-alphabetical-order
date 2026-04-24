package com.insight.launcher.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.insight.launcher.data.ImageCacheManager
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase

class MainViewModelFactory(
    private val application: Application,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val imageCacheManager: ImageCacheManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, getInstalledAppsUseCase, imageCacheManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
