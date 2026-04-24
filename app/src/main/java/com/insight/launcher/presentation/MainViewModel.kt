package com.insight.launcher.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.insight.launcher.data.ImageCacheManager
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase
import com.insight.launcher.presentation.model.AppUiModel

sealed class BackgroundState {
    object Loading : BackgroundState()
    data class Success(val base64: String, val isRotation: Boolean) : BackgroundState()
    object Error : BackgroundState()
}

class MainViewModel(
    application: Application,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val imageCacheManager: ImageCacheManager
) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppUiModel>>()
    val apps: LiveData<List<AppUiModel>> = _apps

    private val _backgroundState = MutableLiveData<BackgroundState>()
    val backgroundState: LiveData<BackgroundState> = _backgroundState

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

    fun loadBackground(isRotation: Boolean = false) {
        val base64Image = if (isRotation) {
            imageCacheManager.rotateImages()
        } else {
            imageCacheManager.getCurrentBase64() ?: imageCacheManager.rotateImages()
        }

        if (base64Image != null) {
            _backgroundState.value = BackgroundState.Success(base64Image, isRotation)
            
            if (isRotation || imageCacheManager.getNextBase64() == null) {
                imageCacheManager.prefetchNextImage()
            }
            return
        }

        _backgroundState.value = BackgroundState.Loading
        
        imageCacheManager.prefetchNextImage { success ->
            if (success) {
                loadBackground(true)
            } else {
                _backgroundState.postValue(BackgroundState.Error)
            }
        }
    }
}
