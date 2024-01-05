package com.xayah.feature.main.home.cloud.page.account

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.CleanablePasswordTextField
import com.xayah.core.ui.component.CleanableTextField
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.emphasizedOffset
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.home.premium.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloudAccount(navController: NavHostController) {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFab(
                expanded = true,
                icon = ImageVectorToken.fromVector(Icons.Rounded.Check),
                text = if (uiState.currentAccountName.isEmpty()) StringResourceToken.fromStringId(R.string.confirm) else StringResourceToken.fromStringId(R.string.modify),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.Confirm(navController = navController))
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
    ) { _ ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
            item {
                Spacer(modifier = Modifier.height(PaddingTokens.Level3))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                ) {
                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                    FilterChip(
                        enabled = uiState.currentAccountName.isEmpty(),
                        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                        selectedIndex = uiState.typeIndex,
                        list = uiState.typeList.map { it.typeDisplay },
                        onSelected = { index, _ ->
                            viewModel.emitIntent(IndexUiIntent.SetTypeIndex(index = index))
                        },
                        onClick = {}
                    )

                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
            }

            item {
                var text by uiState.currentConfig.name
                val emphasizedState by uiState.currentConfig.nameEmphasizedState
                val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

                CleanableTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(PaddingTokens.Level3)
                        .offset(x = emphasizedOffset),
                    value = StringResourceToken.fromString(text),
                    placeholder = StringResourceToken.fromStringId(R.string.name),
                    enabled = uiState.currentAccountName.isEmpty(),
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_badge),
                    prefix = null,
                    onCleanClick = { text = "" },
                ) {
                    text = it
                }
            }

            items(items = uiState.currentConfig.commonTextFields) { config ->
                var text by config.value
                val emphasizedState by config.emphasizedState
                val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

                when (config.keyboardOptions.keyboardType) {
                    KeyboardType.Password -> {
                        CleanablePasswordTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level3)
                                .offset(x = emphasizedOffset),
                            value = StringResourceToken.fromString(text),
                            placeholder = config.placeholder,
                            enabled = true,
                            leadingIcon = config.leadingIcon,
                            keyboardOptions = config.keyboardOptions,
                            prefix = if (config.prefix == null) null else StringResourceToken.fromString(config.prefix),
                            onCleanClick = {
                                text = ""
                            },
                        ) {
                            text = it
                        }
                    }

                    else -> {
                        CleanableTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level3)
                                .offset(x = emphasizedOffset),
                            value = StringResourceToken.fromString(text),
                            placeholder = config.placeholder,
                            enabled = true,
                            leadingIcon = config.leadingIcon,
                            keyboardOptions = config.keyboardOptions,
                            prefix = if (config.prefix == null) null else StringResourceToken.fromString(config.prefix),
                            onCleanClick = {
                                text = ""
                            },
                        ) {
                            text = it
                        }
                    }
                }
            }

            items(items = uiState.currentConfig.extraTextFields) { config ->
                var text by config.value
                val emphasizedState by config.emphasizedState
                val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

                when (config.keyboardOptions.keyboardType) {
                    KeyboardType.Password -> {
                        CleanablePasswordTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level3)
                                .offset(x = emphasizedOffset),
                            value = StringResourceToken.fromString(text),
                            placeholder = config.placeholder,
                            enabled = true,
                            leadingIcon = config.leadingIcon,
                            keyboardOptions = config.keyboardOptions,
                            prefix = if (config.prefix == null) null else StringResourceToken.fromString(config.prefix),
                            onCleanClick = {
                                text = ""
                            },
                        ) {
                            text = it
                        }
                    }

                    else -> {
                        CleanableTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level3)
                                .offset(x = emphasizedOffset),
                            value = StringResourceToken.fromString(text),
                            placeholder = config.placeholder,
                            enabled = true,
                            leadingIcon = config.leadingIcon,
                            keyboardOptions = config.keyboardOptions,
                            prefix = if (config.prefix == null) null else StringResourceToken.fromString(config.prefix),
                            onCleanClick = {
                                text = ""
                            },
                        ) {
                            text = it
                        }
                    }
                }
            }
        }
    }
}
