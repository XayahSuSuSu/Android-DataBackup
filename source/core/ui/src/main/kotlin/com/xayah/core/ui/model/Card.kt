package com.xayah.core.ui.model

import com.xayah.core.model.OperationState
import com.xayah.core.ui.util.fromString

data class ProcessingCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val title: StringResourceToken = StringResourceToken.fromString(""),
    val content: StringResourceToken = StringResourceToken.fromString(""),
    val log: StringResourceToken = StringResourceToken.fromString(""),
)

data class ProcessingPackageCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val title: StringResourceToken = StringResourceToken.fromString(""),
    val packageName: String = "",
    val items: List<ProcessingCardItem> = listOf(),
)

data class ProcessingMediaCardItem(
    val state: OperationState = OperationState.IDLE,
    val progress: Float = -1f,
    val title: StringResourceToken = StringResourceToken.fromString(""),
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