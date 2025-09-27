package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val DisabledOpacity = 0.38f

@Composable
fun SwitchablePreference(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    checked: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = DisabledOpacity),
        label = "animatedColor"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledOpacity),
        label = "animatedColor"
    )
    val animatedSubtitleColor by animateColorAsState(
        targetValue = if (enabled)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(DisabledOpacity),
        label = "animatedColor"
    )


    Surface(modifier = modifier.fillMaxWidth(), enabled = enabled, onClick = { onCheckedChange?.invoke(checked.not()) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                tint = animatedIconColor,
                imageVector = icon,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = animatedTitleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedSubtitleColor
                )
            }
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) ImageVector.vectorResource(R.drawable.ic_check) else ImageVector.vectorResource(R.drawable.ic_x),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            )
        }
    }
}

@Composable
fun SwitchablePreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    title: String,
    subtitle: String,
    dataStorePair: Pair<Preferences.Key<Boolean>, Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val option by context.readBoolean(dataStorePair).collectAsStateWithLifecycle(initialValue = dataStorePair.second)

    SwitchablePreference(
        modifier = modifier,
        enabled = enabled,
        checked = option,
        icon = icon,
        title = title,
        subtitle = subtitle,
    ) {
        scope.launch(Dispatchers.Default) {
            context.saveBoolean(dataStorePair.first, option.not())
        }
    }
}
