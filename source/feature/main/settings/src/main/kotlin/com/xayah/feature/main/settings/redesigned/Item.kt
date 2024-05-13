package com.xayah.feature.main.settings.redesigned

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readThemeType
import com.xayah.core.datastore.saveThemeType
import com.xayah.core.model.ThemeType
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.select
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.settings.R

@ExperimentalAnimationApi
@Composable
fun DarkThemeSelectable() {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val items = remember {
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
    }
    val currentType by context.readThemeType().collectAsStateWithLifecycle(initialValue = ThemeType.AUTO)
    val currentIndex by remember(currentType) { mutableIntStateOf(items.indexOfFirst { it.enum == currentType }) }
    Selectable(
        title = StringResourceToken.fromStringId(R.string.dark_theme),
        value = items[currentIndex].desc,
        current = items[currentIndex].title
    ) {
        val selectedIndex = dialogState.select(
            title = StringResourceToken.fromStringId(R.string.dark_theme),
            defIndex = currentIndex,
            items = items
        )
        context.saveThemeType(items[selectedIndex].enum)
    }
}