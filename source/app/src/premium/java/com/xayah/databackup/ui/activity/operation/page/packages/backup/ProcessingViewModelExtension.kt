package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.getValue
import com.xayah.databackup.service.OperationLocalService
import com.xayah.librootservice.util.withIOContext

suspend fun ProcessingViewModel.backupPackagesExtension() {
    val uiState by uiState

    withIOContext {
        val operationLocalService = OperationLocalService(context = context, cloudMode = true)
        val preparation = operationLocalService.backupPackagesPreparation()

        setEffectState(ProcessingState.Processing)
        operationLocalService.backupPackages(timestamp = uiState.timestamp)

        setEffectState(ProcessingState.Waiting)
        operationLocalService.backupPackagesAfterwards(preparation = preparation)

        operationLocalService.destroyService()
        setEffectFinished()
    }
}