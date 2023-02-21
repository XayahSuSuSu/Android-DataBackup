package com.xayah.databackup.ui.activity.processing.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.data.TaskState

@ExperimentalMaterial3Api
@Composable
fun Task(
    icon: Painter,
    appName: String,
    taskState: TaskState,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconTinySize = dimensionResource(R.dimen.icon_tiny_size)
    val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
    val processingTaskWidth = dimensionResource(R.dimen.processing_task_width)
    val colorYellow = colorResource(id = R.color.yellow)
    val colorGreen = colorResource(id = R.color.green)
    val colorError = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .padding(mediumPadding)
            .width(processingTaskWidth)
            .clip(RoundedCornerShape(mediumPadding))
            .clickable {
                if (clickable) onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(iconMediumSize),
            painter = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(
                nonePadding,
                tinyPadding,
                nonePadding,
                nonePadding
            ),
            text = appName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        when (taskState) {
            TaskState.Processing -> {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_bolt),
                    contentDescription = null,
                    tint = colorYellow,
                    modifier = Modifier
                        .size(iconTinySize)
                        .padding(
                            nonePadding,
                            tinyPadding,
                            nonePadding,
                            nonePadding
                        )
                )
            }
            TaskState.Success -> {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    tint = colorGreen,
                    modifier = Modifier
                        .size(iconTinySize)
                        .padding(
                            nonePadding,
                            tinyPadding,
                            nonePadding,
                            nonePadding
                        )
                )
            }
            TaskState.Failed -> {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = colorError,
                    modifier = Modifier
                        .size(iconTinySize)
                        .padding(
                            nonePadding,
                            tinyPadding,
                            nonePadding,
                            nonePadding
                        )
                )
            }
            else -> {
                Spacer(
                    modifier = Modifier
                        .size(iconTinySize)
                        .padding(
                            nonePadding,
                            tinyPadding,
                            nonePadding,
                            nonePadding
                        )
                )
            }
        }
    }
}
