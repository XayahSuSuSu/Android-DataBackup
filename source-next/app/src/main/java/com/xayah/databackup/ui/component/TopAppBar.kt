package com.xayah.databackup.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

private var cachedDefaultLargeTopAppBarColors: TopAppBarColors? = null

@Composable
fun TopAppBarDefaults.defaultLargeTopAppBarColors(): TopAppBarColors = cachedDefaultLargeTopAppBarColors ?: largeTopAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    scrolledContainerColor = MaterialTheme.colorScheme.surface
).also { cachedDefaultLargeTopAppBarColors = it }
