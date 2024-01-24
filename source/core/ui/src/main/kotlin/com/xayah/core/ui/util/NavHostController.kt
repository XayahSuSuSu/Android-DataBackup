package com.xayah.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

val LocalNavController: ProvidableCompositionLocal<NavHostController?> = staticCompositionLocalOf { null }

fun NavHostController.navigateAndPopBackStack(route: String) = navigate(route) { popBackStack() }

@Composable
fun NavHostController.currentRoute() = currentBackStackEntryAsState().value?.destination?.route
