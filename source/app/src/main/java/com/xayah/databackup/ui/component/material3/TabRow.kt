/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xayah.databackup.ui.component.material3

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

private enum class TabSlots {
    Tabs,
    Divider,
    Indicator
}

/**
 * Data class that contains information about a tab's position on screen, used for calculating
 * where to place the indicator that shows which tab is selected.
 *
 * @property left the left edge's x position from the start of the [TabRow]
 * @property right the right edge's x position from the start of the [TabRow]
 * @property width the width of this tab
 */
@Immutable
class TabPosition internal constructor(val left: Dp, val width: Dp) {
    val right: Dp get() = left + width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabPosition) return false

        if (left != other.left) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + width.hashCode()
        return result
    }

    override fun toString(): String {
        return "TabPosition(left=$left, right=$right, width=$width)"
    }
}

/**
 * [Modifier] that takes up all the available width inside the [TabRow], and then animates
 * the offset of the indicator it is applied to, depending on the [currentTabPosition].
 *
 * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
 * calculate the offset of the indicator this modifier is applied to, as well as its width.
 */
fun Modifier.tabIndicatorOffset(
    currentTabPosition: TabPosition,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(currentTabWidth)
}

// TODO: Provide M3 tab row asset and docs when available.
/**
 * Material Design fixed tabs.
 *
 * Fixed tabs display all tabs in a set simultaneously. They are best for switching between related
 * content quickly, such as between transportation methods in a map. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A TabRow places its tabs evenly spaced along the entire row, with each tab
 * taking up an equal amount of space. See [ScrollableTabRow] for a tab row that does not enforce
 * equal size, and allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material3.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize
 * the indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it
 * should internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material3.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator,
 * as we aren't changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the
 * appearance of the indicator as it animates between tabs, such as changing its color or size.
 * [indicator] is stacked on top of the entire TabRow, so you just need to provide a custom
 * transition that animates the offset of the indicator from the start of the TabRow. For
 * example, take the following example that uses a transition to animate the offset, width, and
 * color of the same FancyIndicator from before, also adding a physics based 'spring' effect to
 * the indicator in the direction of motion:
 *
 * @sample androidx.compose.material3.samples.FancyAnimatedIndicator
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorContainerTabs
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 * to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 * will be a [TabRowDefaults.Indicator], using a [TabRowDefaults.tabIndicatorOffset] modifier to
 * animate its position. Note that this indicator will be forced to fill up the entire tab row, so
 * you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual drawn
 * indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 * separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 * inside this lambda will be measured and placed evenly across the row, each taking up equal space.
 */
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.containerColor,
    contentColor: Color = TabRowDefaults.contentColor,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
        if (selectedTabIndex < tabPositions.size) {
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
            )
        }
    },
    divider: @Composable () -> Unit = @Composable {
        Divider()
    },
    tabs: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)
            val tabCount = tabMeasurables.size
            var tabWidth = 0
            if (tabCount > 0) {
                tabWidth = (tabRowWidth / tabCount)
            }
            val tabRowHeight = tabMeasurables.fold(initial = 0) { max, curr ->
                maxOf(curr.maxIntrinsicHeight(tabWidth), max)
            }

            val tabPlaceables = tabMeasurables.map {
                it.measure(
                    constraints.copy(
                        minWidth = tabWidth,
                        maxWidth = tabWidth,
                        minHeight = tabRowHeight,
                        maxHeight = tabRowHeight,
                    )
                )
            }

            val tabPositions = List(tabCount) { index ->
                TabPosition(tabWidth.toDp() * index, tabWidth.toDp())
            }

            layout(tabRowWidth, tabRowHeight) {
                subcompose(TabSlots.Indicator) {
                    indicator(tabPositions)
                }.forEach {
                    it.measure(Constraints.fixed(tabRowWidth, tabRowHeight)).placeRelative(0, 0)
                }

                subcompose(TabSlots.Divider, divider).forEach {
                    val placeable = it.measure(constraints.copy(minHeight = 0))
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                tabPlaceables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }
            }
        }
    }
}