package com.xayah.core.util

import androidx.navigation.NavController
import androidx.navigation.NavHostController

fun NavHostController.maybePopBackStack() = if (previousBackStackEntry != null) popBackStack() else false
fun NavHostController.navigateSingle(route: String) = navigate(route) { popUpTo(route) { inclusive = true } }

fun NavController.maybePopBackStack() = if (previousBackStackEntry != null) popBackStack() else false
fun NavController.navigateSingle(route: String) = navigate(route) { popUpTo(route) { inclusive = true } }
fun NavController.maybePopBackAndNavigateSingle(route: String) {
    maybePopBackStack()
    navigateSingle(route)
}