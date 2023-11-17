package com.xayah.feature.guide.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

@ExperimentalMaterial3Api
val LocalMainViewModel: ProvidableCompositionLocal<MainViewModel?> = staticCompositionLocalOf { null }
