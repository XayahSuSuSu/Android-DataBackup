package com.xayah.databackup.ui.activity.list.blacklist.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.blacklist.BlackListViewModel
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun BlackListScaffold(
    viewModel: BlackListViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val list = viewModel.blackList.collectAsState()

    LaunchedEffect(null) {
        viewModel.initializeList(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(mediumPadding),
                onClick = {
                    scope.launch {
                        viewModel.importConfig(context)
                    }
                },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_download),
                    contentDescription = null
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = stringResource(id = R.string.blacklist))
                },
                scrollBehavior = this,
                navigationIcon = {
                    IconButton(icon = Icons.Rounded.ArrowBack, onClick = onFinish)
                },
            )
        },
        topPaddingRate = 1,
        content = {
            items(list.value) {
                BlackListItemClickable(
                    title = it.appName,
                    subtitle = it.packageName,
                    onClick = {},
                    onIconButtonClick = {
                        scope.launch {
                            viewModel.removeItem(context, it)
                        }
                    })
            }
        }
    )
}
