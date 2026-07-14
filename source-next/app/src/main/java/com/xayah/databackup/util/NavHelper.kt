package com.xayah.databackup.util

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events by updating the Navigation 3 back stack.
 */
class Navigator(
    private val backStack: NavBackStack<NavKey>,
) {
    fun navigate(route: NavKey) {
        if (backStack.lastOrNull()?.let { it::class == route::class } != true) {
            backStack.add(route)
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}

/**
 * Navigate to target route with debounce handled.
 */
fun Navigator.navigateSafely(route: NavKey) {
    navigate(route)
}

/**
 * Pop back stack safely.
 */
fun Navigator.popBackStackSafely() {
    goBack()
}
