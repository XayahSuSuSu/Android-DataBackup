package com.xayah.databackup.ui.activity.main.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R

@Composable
fun MainBottomBar() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = stringArrayResource(id = R.array.bottom_bar_items)
    val icons = listOf(
        ImageVector.vectorResource(R.drawable.ic_rounded_acute),
        ImageVector.vectorResource(R.drawable.ic_rounded_history),
        ImageVector.vectorResource(R.drawable.ic_rounded_cloud_upload),
        ImageVector.vectorResource(R.drawable.ic_rounded_settings),
    )
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}