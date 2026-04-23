package com.insight.launcher.domain.usecase

import com.insight.launcher.domain.model.AppModel
import com.insight.launcher.domain.repository.AppRepository

class GetInstalledAppsUseCase(private val repository: AppRepository) {
    operator fun invoke(): List<AppModel> = repository.getInstalledApps().sortedBy { it.label }
}
