package com.xayah.databackup.compose.ui.activity.main.components

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.list.ListActivity
import com.xayah.databackup.compose.ui.activity.settings.SettingsActivity
import com.xayah.databackup.compose.ui.components.Scaffold
import com.xayah.databackup.compose.ui.components.animation.ContentFade
import com.xayah.databackup.compose.ui.components.animation.ContentFadeSlideHorizontallyAnimation
import com.xayah.databackup.data.*

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
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = this,
                actions = {
                    ContentFade(isInitialized) {
                        IconButton(onClick = {
                            context.startActivity(
                                Intent(context, SettingsActivity::class.java)
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(id = R.string.settings)
                            )
                        }
                    }
                },
            )
        },
        topPaddingRate = 1,
        content = {
            if (isInitialized.targetState.not()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            item {
                ContentFadeSlideHorizontallyAnimation(isInitialized) {
                    val colorYellow = colorResource(id = R.color.yellow)
                    ItemCard(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_round_apps),
                        iconTint = colorYellow,
                        title = stringResource(id = R.string.application),
                        subtitle = stringResource(R.string.card_app_subtitle),
                        onBackupClick = {
                            context.startActivity(
                                Intent(context, ListActivity::class.java).apply {
                                    putExtra(
                                        TypeActivityTag,
                                        TypeBackupApp
                                    )
                                })
                        },
                        onRestoreClick = {
                            context.startActivity(
                                Intent(context, ListActivity::class.java).apply {
                                    putExtra(
                                        TypeActivityTag,
                                        TypeRestoreApp
                                    )
                                })
                        }
                    )
                }
            }
            item {
                ContentFadeSlideHorizontallyAnimation(isInitialized) {
                    val colorBlue = colorResource(id = R.color.blue)
                    ItemCard(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_round_image),
                        iconTint = colorBlue,
                        title = stringResource(id = R.string.media),
                        subtitle = stringResource(R.string.card_media_subtitle),
                        onBackupClick = {
                            context.startActivity(
                                Intent(context, ListActivity::class.java).apply {
                                    putExtra(
                                        TypeActivityTag,
                                        TypeBackupMedia
                                    )
                                })
                        },
                        onRestoreClick = {
                            context.startActivity(
                                Intent(context, ListActivity::class.java).apply {
                                    putExtra(
                                        TypeActivityTag,
                                        TypeRestoreMedia
                                    )
                                })
                        }
                    )
                }
            }
        }
    )
}
