package com.xayah.databackup.feature

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xayah.databackup.feature.dashboard.DashboardScreen
import com.xayah.databackup.feature.settings.SettingsScreen
import com.xayah.databackup.ui.component.FloatingNavigationBar
import com.xayah.databackup.ui.component.FloatingNavigationItem
import com.xayah.databackup.ui.component.FloatingNavigationItems
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.util.Navigator
import kotlinx.coroutines.launch

private const val PageAnimationDurationMillis = 400
private val MainNavigationItems = FloatingNavigationItems.filter { item ->
    item == FloatingNavigationItem.HOME || item == FloatingNavigationItem.SETTINGS
}

@Composable
fun MainNavigationHost(navigator: Navigator) {
    val pagerState = rememberPagerState(pageCount = MainNavigationItems::size)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var floatingNavigationBarBottomPadding by remember { mutableStateOf(0.dp) }

    fun animateToItem(item: FloatingNavigationItem) {
        val page = MainNavigationItems.indexOf(item)
        if (page < 0) {
            return
        }
        if (pagerState.targetPage == page) {
            return
        }
        coroutineScope.launch {
            pagerState.animateScrollToPage(page = page, animationSpec = tween(durationMillis = PageAnimationDurationMillis, easing = EaseInOut))
        }
    }

    BackHandler(enabled = MainNavigationItems[pagerState.currentPage] != FloatingNavigationItem.HOME) {
        animateToItem(FloatingNavigationItem.HOME)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalFloatingNavigationBarBottomPadding provides floatingNavigationBarBottomPadding) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = true,
            ) { page ->
                when (MainNavigationItems[page]) {
                    FloatingNavigationItem.HOME -> DashboardScreen(navigator)
                    FloatingNavigationItem.SETTINGS -> SettingsScreen()
                    else -> Unit
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        val measuredPadding = with(density) { size.height.toDp() }
                        if (floatingNavigationBarBottomPadding != measuredPadding) {
                            floatingNavigationBarBottomPadding = measuredPadding
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                FloatingNavigationBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    selectedItem = MainNavigationItems[pagerState.targetPage],
                    onSelected = ::animateToItem,
                )
            }
        }
    }
}
