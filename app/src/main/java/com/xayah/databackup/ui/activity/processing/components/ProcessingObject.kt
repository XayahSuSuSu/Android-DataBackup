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
import com.xayah.databackup.R
import com.xayah.databackup.data.DataType
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.ui.components.TitleMediumText
import com.xayah.databackup.ui.components.paddingEnd

@ExperimentalMaterial3Api
@Composable
fun ProcessObject(cardState: TaskState, title: String, subtitle: String, type: DataType) {
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
                    DataType.APK -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_android)
                    }
                    DataType.USER -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_person)
                    }
                    DataType.USER_DE -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_person)
                    }
                    DataType.DATA -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_database)
                    }
                    DataType.OBB -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_esports)
                    }
                    DataType.APP_MEDIA -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_image)
                    }
                    else -> {
                        ImageVector.vectorResource(id = R.drawable.ic_round_person)
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
                    .paddingEnd(smallPadding)
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
                TitleMediumText(
                    text = type.type.uppercase(),
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
