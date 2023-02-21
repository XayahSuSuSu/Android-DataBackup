package com.xayah.databackup.ui.activity.processing.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R
import com.xayah.databackup.data.*

data class ProcessObjectItem(
    val state: TaskState,
    val visible: Boolean,
    val title: String,
    val subtitle: String,
    val type: ProcessingObjectType,
)

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun ProcessObjectPreview() {
    ProcessObject(
        cardState = TaskState.Success,
        visible = true,
        title = "校验中",
        subtitle = "大小: 138Mib, 速度: 312Mib/s",
        type = ProcessingObjectType.APP,
    )
}

@ExperimentalMaterial3Api
@Composable
fun ProcessObject(
    cardState: TaskState,
    visible: Boolean,
    title: String,
    subtitle: String,
    type: ProcessingObjectType,
) {
    if (visible) {
        val nonePadding = dimensionResource(R.dimen.padding_none)
        val smallPadding = dimensionResource(R.dimen.padding_small)
        val bigPadding = dimensionResource(R.dimen.padding_big)
        val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
        val iconSmallSize = dimensionResource(R.dimen.icon_tiny_size)
        val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        val colorError = MaterialTheme.colorScheme.error
        val colorErrorContainer = MaterialTheme.colorScheme.errorContainer
        val colorGreen = colorResource(id = R.color.green)
        val colorGreenContainer = colorResource(id = R.color.greenContainer)

        Card(
            modifier = Modifier
                .clickable {}
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (cardState) {
                    TaskState.Waiting, TaskState.Processing -> {
                        colorSurfaceVariant
                    }
                    TaskState.Success -> {
                        colorGreen
                    }
                    TaskState.Failed -> {
                        colorError
                    }
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(bigPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (type) {
                        ProcessingObjectType.APP -> {
                            ImageVector.vectorResource(id = R.drawable.ic_round_android)
                        }
                        ProcessingObjectType.USER -> {
                            ImageVector.vectorResource(id = R.drawable.ic_round_person)
                        }
                        ProcessingObjectType.USER_DE -> {
                            ImageVector.vectorResource(id = R.drawable.ic_round_person)
                        }
                        ProcessingObjectType.DATA -> {
                            ImageVector.vectorResource(id = R.drawable.ic_round_database)
                        }
                        ProcessingObjectType.OBB -> {
                            ImageVector.vectorResource(id = R.drawable.ic_round_esports)
                        }
                    },
                    contentDescription = null,
                    tint = when (cardState) {
                        TaskState.Waiting, TaskState.Processing -> {
                            colorOnSurfaceVariant
                        }
                        TaskState.Success -> {
                            colorGreenContainer
                        }
                        TaskState.Failed -> {
                            colorErrorContainer
                        }
                    },
                    modifier = Modifier
                        .padding(nonePadding, nonePadding, smallPadding, nonePadding)
                        .size(iconMediumSize)
                        .clip(CircleShape)
                        .padding(smallPadding)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (cardState) {
                            TaskState.Waiting, TaskState.Processing -> {
                                colorOnSurfaceVariant
                            }
                            TaskState.Success -> {
                                colorGreenContainer
                            }
                            TaskState.Failed -> {
                                colorErrorContainer
                            }
                        }
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = when (cardState) {
                            TaskState.Waiting, TaskState.Processing -> {
                                colorOnSurfaceVariant
                            }
                            TaskState.Success -> {
                                colorGreenContainer
                            }
                            TaskState.Failed -> {
                                colorErrorContainer
                            }
                        }
                    )
                }

                if (cardState == TaskState.Processing)
                    CircularProgressIndicator(modifier = Modifier.size(iconSmallSize))
                else
                    Text(
                        text = when (type) {
                            ProcessingObjectType.APP -> {
                                ProcessingItemTypeAPK
                            }
                            ProcessingObjectType.USER -> {
                                ProcessingItemTypeUSER
                            }
                            ProcessingObjectType.USER_DE -> {
                                ProcessingItemTypeUSERDE
                            }
                            ProcessingObjectType.DATA -> {
                                ProcessingItemTypeDATA
                            }
                            ProcessingObjectType.OBB -> {
                                ProcessingItemTypeOBB
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (cardState) {
                            TaskState.Waiting, TaskState.Processing -> {
                                colorOnSurfaceVariant
                            }
                            TaskState.Success -> {
                                colorGreenContainer
                            }
                            TaskState.Failed -> {
                                colorErrorContainer
                            }
                        }
                    )
            }
        }
    }
}
