package com.xayah.databackup.ui.activity.directory.page

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.model.OpType
import com.xayah.databackup.ui.component.DirectoryScaffold
import com.xayah.databackup.ui.component.ListItemDirectory
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageDirectory(title: String, opType: OpType) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<DirectoryViewModel>()
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState
    val directories by uiState.directories.collectAsState(initial = listOf())

    LaunchedEffect(null) {
        viewModel.initialize(context = context, opType = opType)
    }

    DirectoryScaffold(
        title = title,
        onFabClick = {
            viewModel.onAdd(context = (context as ComponentActivity))
        }
    ) {
        Loader(modifier = Modifier.fillMaxSize(), isLoading = uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                item {
                    Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                }

                items(items = directories, key = { it.id }) { item ->
                    ListItemDirectory(
                        entity = item,
                        onCardClick = {
                            scope.launch {
                                withIOContext {
                                    viewModel.select(context = context, path = item.path, type = opType, id = item.id)
                                }
                            }
                        },
                        chipGroup = {
                            for (tag in item.tags) {
                                if (tag.isNotEmpty()) Serial(serial = tag, enabled = item.enabled)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
