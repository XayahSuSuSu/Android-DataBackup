package com.xayah.databackup.util

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController

/**
 * Navigate to target route with debounce handled.
 */
inline fun <reified T : Any> NavHostController.navigateSafely(route: T) {
    if (currentDestination?.hasRoute<T>() != true) {
        navigate(route) { launchSingleTop = true }
    }
}

/**
 * Pop back stack safely.
 */
fun NavHostController.popBackStackSafely() {
    navigateUp()
}
