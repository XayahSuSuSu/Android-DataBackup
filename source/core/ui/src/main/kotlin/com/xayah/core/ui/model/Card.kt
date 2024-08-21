package com.xayah.core.ui.model

import com.xayah.core.model.OperationState

data class ProcessingCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val title: String = "",
    val content: String = "",
    val log: String = "",
)

data class ProcessingDataCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val processingIndex: Int = 0,
    val title: String = "",
    val key: String = "",
    val items: List<ProcessingCardItem> = listOf(),
)

data class ProcessingMediaCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val title: String = "",
    val name: String = "",
    val items: List<ProcessingCardItem> = listOf(),
)

data class ReportAppItemInfo(
    val packageName: String,
    val index: Int,
    val label: String,
    val user: String,
)

data class ReportFileItemInfo(
    val name: String,
    val index: Int,
)