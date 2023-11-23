package com.xayah.feature.crash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.core.ui.theme.JetbrainsMonoFamily
import com.xayah.core.ui.token.PaddingTokens
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val crashInfo = intent.getStringExtra("crashInfo") ?: ""

        setContent {
            DataBackupTheme {
                val viewModel = hiltViewModel<IndexViewModel>()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(null) {
                    viewModel.emitState(uiState.copy(text = crashInfo))
                }

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(PaddingTokens.Level3)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                    ) {
                        InnerTopSpacer(innerPadding = innerPadding)

                        // TopBar
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                            modifier = Modifier
                                .size(PaddingTokens.Level6)
                                .paddingBottom(PaddingTokens.Level1)
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
