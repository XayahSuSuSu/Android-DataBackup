package com.xayah.databackup.ui.activity.main.page

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.ViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.log.LogViewModel
import com.xayah.databackup.ui.activity.main.page.reload.ReloadViewModel
import com.xayah.databackup.ui.activity.main.page.tree.TreeViewModel
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.LogTopBar
import com.xayah.databackup.ui.component.MainBottomBar
import com.xayah.databackup.ui.component.MainTopBar
import com.xayah.databackup.ui.component.ReloadTopBar
import com.xayah.databackup.ui.component.TreeTopBar
import com.xayah.databackup.ui.component.openFileOpDialog
import com.xayah.core.util.DateUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.core.common.util.toLineString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
sealed class MainUiState(
    val scrollBehavior: TopAppBarScrollBehavior?,
    val topBar: @Composable () -> Unit = {},
    val bottomBar: @Composable (() -> Unit)?,
    val floatingActionButton: @Composable () -> Unit = {},
    val floatingActionButtonPosition: FabPosition = FabPosition.End,
) {
    class Main(scrollBehavior: TopAppBarScrollBehavior?) : MainUiState(
        scrollBehavior = scrollBehavior,
        topBar = {
            MainTopBar(scrollBehavior = scrollBehavior)
        },
        bottomBar = {
            MainBottomBar()
        }
    )

    class Tree(scrollBehavior: TopAppBarScrollBehavior?, viewModel: TreeViewModel) : MainUiState(
        scrollBehavior = scrollBehavior,
        topBar = {
            TreeTopBar(scrollBehavior = scrollBehavior, viewModel = viewModel)
        },
        bottomBar = {
            MainBottomBar()
        }
    )

    class Log(scrollBehavior: TopAppBarScrollBehavior?, viewModel: LogViewModel) : MainUiState(
        scrollBehavior = scrollBehavior,
        topBar = {
            LogTopBar(scrollBehavior = scrollBehavior, viewModel = viewModel)
        },
        bottomBar = null,
        floatingActionButton = {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val dialogSlot = LocalSlotScope.current!!.dialogSlot
            val uiState by viewModel.uiState
            val logText = uiState.logTextList.toLineString()
            val selectedIndex = uiState.selectedIndex
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        if (selectedIndex != -1) {
                            val filePath = PathUtil.getLogSavePath(timestamp = DateUtil.getTimestamp())
                            dialogSlot.openFileOpDialog(
                                context = context,
                                title = context.getString(R.string.export),
                                filePath = filePath,
                                icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_save),
                                text = logText
                            )
                        }
                    }
                },
                expanded = true,
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_save),
                        contentDescription = null
                    )
                },
                text = { Text(text = stringResource(id = R.string.export)) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    )

    class Reload(scrollBehavior: TopAppBarScrollBehavior?, viewModel: ReloadViewModel) : MainUiState(
        scrollBehavior = scrollBehavior,
        topBar = {
            ReloadTopBar(scrollBehavior = scrollBehavior, viewModel = viewModel)
        },
        bottomBar = {
            MainBottomBar()
        }
    )
}

@ExperimentalMaterial3Api
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf<MainUiState>(MainUiState.Main(null))
    val uiState: State<MainUiState>
        get() = _uiState

    fun toUiState(uiState: MainUiState) {
        _uiState.value = uiState
    }
}
