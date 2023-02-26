package com.xayah.databackup.ui.activity.blacklist.components

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.blacklist.BlackListViewModel
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.readBlackListMapPath
import com.xayah.databackup.util.saveBlackListMapPath
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch

suspend fun initializeList(viewModel: BlackListViewModel, context: Context) {
    viewModel.blackList.value.clear()
    viewModel.blackList.value.addAll(Command.readBlackListMap(context.readBlackListMapPath()).values)
}

@ExperimentalMaterial3Api
@Composable
fun BlackListScaffold(
    viewModel: BlackListViewModel,
    explorer: MaterialYouFileExplorer,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val list = viewModel.blackList.collectAsState()

    LaunchedEffect(null) {
        initializeList(viewModel, context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(mediumPadding),
                onClick = {
                    explorer.apply {
                        isFile = true
                        toExplorer(context) { path, _ ->
                            context.saveBlackListMapPath(path)
                            scope.launch {
                                initializeList(viewModel, context)
                            }
                        }
                    }
                },
            ) {
                Icon(ImageVector.vectorResource(id = R.drawable.ic_round_download), null)
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.blacklist),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = this,
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = com.xayah.materialyoufileexplorer.R.drawable.ic_round_arrow_back),
                            contentDescription = null
                        )
                    }
                },
            )
        },
        topPaddingRate = 1,
        content = {
            items(count = list.value.size) {
                BlackListItemClickable(
                    title = list.value[it].appName,
                    subtitle = list.value[it].packageName,
                    onClick = {},
                    onIconButtonClick = {
                        scope.launch {
                            Command.removeBlackList(
                                context.readBlackListMapPath(),
                                list.value[it].packageName
                            )
                            viewModel.blackList.value.removeAt(it)
                            GlobalObject.getInstance().appInfoBackupMap.value.clear()
                        }
                    })
            }
        }
    )
}
