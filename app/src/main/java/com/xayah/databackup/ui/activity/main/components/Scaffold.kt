package com.xayah.databackup.ui.activity.main.components

import android.content.Intent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.common.CommonListActivity
import com.xayah.databackup.ui.activity.list.telephony.TelephonyActivity
import com.xayah.databackup.ui.activity.settings.SettingsActivity
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle
import com.xayah.databackup.ui.components.animation.ContentFade

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MainScaffold(
    isInitialized: MutableTransitionState<Boolean>
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopBarTitle(text = stringResource(id = R.string.app_name)) },
                scrollBehavior = this,
                actions = {
                    ContentFade(isInitialized) {
                        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = stringResource(id = R.string.settings))
                        }
                    }
                },
            )
        },
        topPaddingRate = 1,
        isInitialized = isInitialized,
        content = {
            item {
                val colorYellow = colorResource(id = R.color.yellow)
                ItemCard(
                    icon = rememberDrawablePainter(drawable = AppCompatResources.getDrawable(context, R.drawable.ic_iconfont_apps)),
                    title = stringResource(id = R.string.application),
                    subtitle = stringResource(R.string.card_app_subtitle),
                    onBackupClick = {
                        context.startActivity(Intent(context, CommonListActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeBackupApp)
                        })
                    },
                    onRestoreClick = {
                        context.startActivity(Intent(context, CommonListActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeRestoreApp)
                        })
                    }
                )
            }
            item {
                val colorBlue = colorResource(id = R.color.blue)
                ItemCard(
                    icon = rememberDrawablePainter(drawable = AppCompatResources.getDrawable(context, R.drawable.ic_iconfont_media_folder)),
                    title = stringResource(id = R.string.media),
                    subtitle = stringResource(R.string.card_media_subtitle),
                    onBackupClick = {
                        context.startActivity(Intent(context, CommonListActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeBackupMedia)
                        })
                    },
                    onRestoreClick = {
                        context.startActivity(Intent(context, CommonListActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeRestoreMedia)
                        })
                    }
                )
            }
            item {
                val colorBlue = colorResource(id = R.color.green)
                ItemCard(
                    icon = rememberDrawablePainter(drawable = AppCompatResources.getDrawable(context, R.drawable.ic_iconfont_message)),
                    title = stringResource(R.string.telephony),
                    subtitle = stringResource(R.string.card_telephony_subtitle),
                    onBackupClick = {
                        context.startActivity(Intent(context, TelephonyActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeBackupTelephony)
                        })
                    },
                    onRestoreClick = {
                        context.startActivity(Intent(context, TelephonyActivity::class.java).apply {
                            putExtra(TypeActivityTag, TypeRestoreTelephony)
                        })
                    }
                )
            }
        }
    )
}
