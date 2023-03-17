package com.xayah.databackup.ui.activity.list.telephony.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle

@ExperimentalPermissionsApi
@ExperimentalMaterial3Api
@Composable
fun TelephonyScaffold(
    viewModel: TelephonyViewModel,
    title: String,
    isFabVisible: Boolean,
    onConfirm: () -> Unit,
    onFinish: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Scaffold(
        floatingActionButton = {
            if (isFabVisible)
                FloatingActionButton(
                    modifier = Modifier.padding(mediumPadding),
                    onClick = onConfirm,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null
                    )
                }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = title)
                },
                scrollBehavior = this,
                navigationIcon = {
                    IconButton(icon = Icons.Rounded.ArrowBack, onClick = onFinish)
                },
            )
        },
        topPaddingRate = 1,
        topSpace = false,
        content = {
            item {
                val state = viewModel.tabRowState
                val titles = stringArrayResource(id = R.array.telephony_type)

                ScrollableTabRow(
                    selectedTabIndex = state.value,
                    edgePadding = nonePadding,
                    indicator = @Composable { tabPositions: List<TabPosition> ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[state.value])
                                .clip(CircleShape)
                        )
                    }
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state.value == index,
                            onClick = { state.value = index },
                            text = {
                                Text(
                                    text = title,
                                )
                            }
                        )
                    }
                }
            }
            content()
        }
    )
}
