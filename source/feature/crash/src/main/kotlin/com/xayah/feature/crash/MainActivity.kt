package com.xayah.feature.crash

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.TopBarTitle
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.core.ui.theme.JetbrainsMonoFamily
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.PaddingTokens
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val crashInfo = intent.getStringExtra("crashInfo") ?: ""

        setContent {
            DataBackupTheme {
                CompositionLocalProvider(
                    androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current,
                ) {

                    val viewModel = hiltViewModel<IndexViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(null) {
                        viewModel.emitState(uiState.copy(text = crashInfo))
                    }

                    Scaffold { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level4)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                        ) {
                            InnerTopSpacer(innerPadding = innerPadding)

                            // TopBar
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                modifier = Modifier
                                    .size(PaddingTokens.Level7)
                                    .paddingBottom(PaddingTokens.Level2)
                            )
                            TopBarTitle(text = stringResource(id = R.string.app_crashed))

                            // Content
                            LabelSmallText(text = uiState.text, fontFamily = JetbrainsMonoFamily)

                            InnerBottomSpacer(innerPadding = innerPadding)
                        }
                    }
                }
            }
        }
    }
}
