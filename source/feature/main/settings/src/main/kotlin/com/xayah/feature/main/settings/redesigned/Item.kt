package com.xayah.feature.main.settings.redesigned

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
import com.xayah.core.ui.util.fromString

@Composable
fun DarkThemeSelectable() {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val items = remember {
        listOf(
            DialogRadioItem(
                enum = ThemeType.AUTO,
                title = StringResourceToken.fromString("Auto"),
                desc = StringResourceToken.fromString("Will turn on automatically"),
            ),
            DialogRadioItem(
                enum = ThemeType.LIGHT_THEME,
                title = StringResourceToken.fromString("Light"),
                desc = StringResourceToken.fromString("Always in light mode"),
            ),
            DialogRadioItem(
                enum = ThemeType.DARK_THEME,
                title = StringResourceToken.fromString("Dark"),
                desc = StringResourceToken.fromString("Always in dark mode"),
            ),
        )
    }
    val currentType by context.readThemeType().collectAsStateWithLifecycle(initialValue = ThemeType.AUTO)
    val currentIndex by remember(currentType) { mutableIntStateOf(items.indexOfFirst { it.enum == currentType }) }
    Selectable(
        title = StringResourceToken.fromString("Dark theme"),
        value = items[currentIndex].desc,
        current = items[currentIndex].title
    ) {
        val selectedIndex = dialogState.select(
            title = StringResourceToken.fromString("Dark theme"),
            defIndex = currentIndex,
            items = items
        )
        context.saveThemeType(items[selectedIndex].enum)
    }
}