package com.xayah.feature.crash

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
                            LabelSmallText(text = uiState.text, fontFamily = JetbrainsMonoFamily, modifier = Modifier.fillMaxWidth())

                            Spacer(modifier = Modifier.height(PaddingTokens.Level4))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End // Align buttons to the end (right)
                            ) {
                                val context = LocalContext.current
                                val clipboardManager = LocalClipboardManager.current

                                OutlinedButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.text))
                                }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(id = R.string.copy_to_clipboard), modifier = Modifier.size(PaddingTokens.Level3))
                                    Spacer(modifier = Modifier.width(PaddingTokens.Level1))
                                    Text(stringResource(id = R.string.copy_to_clipboard))
                                }
                                Spacer(modifier = Modifier.width(PaddingTokens.Level2))
                                Button(onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, uiState.text)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = stringResource(id = R.string.share_report), modifier = Modifier.size(PaddingTokens.Level3))
                                    Spacer(modifier = Modifier.width(PaddingTokens.Level1))
                                    Text(stringResource(id = R.string.share_report))
                                }
                            }

                            InnerBottomSpacer(innerPadding = innerPadding)
                        }
                    }
                }
            }
        }
    }
}
// Add these string resources to source/feature/crash/src/main/res/values/strings.xml
// <string name="copy_to_clipboard">Copy</string>
// <string name="share_report">Share</string>
// R.string.app_crashed is already used from core/ui presumably
