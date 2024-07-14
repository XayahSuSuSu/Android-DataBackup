package com.xayah.feature.main.settings

import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xayah.core.datastore.saveThemeType
import com.xayah.core.model.ThemeType
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.select
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.theme.observeCurrentTheme
import com.xayah.core.ui.util.fromStringId

@ExperimentalAnimationApi
@Composable
fun DarkThemeSelectable() {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val items = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            listOf(
                DialogRadioItem(
                    enum = ThemeType.AUTO,
                    title = StringResourceToken.fromStringId(R.string.theme_auto),
                    desc = StringResourceToken.fromStringId(R.string.theme_auto_desc),
                ),
                DialogRadioItem(
                    enum = ThemeType.LIGHT_THEME,
                    title = StringResourceToken.fromStringId(R.string.theme_light),
                    desc = StringResourceToken.fromStringId(R.string.theme_light_desc),
                ),
                DialogRadioItem(
                    enum = ThemeType.DARK_THEME,
                    title = StringResourceToken.fromStringId(R.string.theme_dark),
                    desc = StringResourceToken.fromStringId(R.string.theme_dark_desc),
                ),
            )
        } else {
            listOf(
                DialogRadioItem(
                    enum = ThemeType.LIGHT_THEME,
                    title = StringResourceToken.fromStringId(R.string.theme_light),
                    desc = StringResourceToken.fromStringId(R.string.theme_light_desc),
                ),
                DialogRadioItem(
                    enum = ThemeType.DARK_THEME,
                    title = StringResourceToken.fromStringId(R.string.theme_dark),
                    desc = StringResourceToken.fromStringId(R.string.theme_dark_desc),
                ),
            )
        }
    }
    val currentType by observeCurrentTheme()
    val currentIndex by remember(currentType) { mutableIntStateOf(items.indexOfFirst { it.enum == currentType }) }
    Selectable(
        title = StringResourceToken.fromStringId(R.string.dark_theme),
        value = items[currentIndex].desc,
        current = items[currentIndex].title
    ) {
        val (state, selectedIndex) = dialogState.select(
            title = StringResourceToken.fromStringId(R.string.dark_theme),
            defIndex = currentIndex,
            items = items
        )
        if (state.isConfirm) {
            context.saveThemeType(items[selectedIndex].enum!!)
        }
    }
}
