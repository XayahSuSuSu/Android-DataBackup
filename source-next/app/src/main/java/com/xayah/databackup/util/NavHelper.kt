package com.xayah.databackup.util

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events by updating the Navigation 3 back stack.
 */
class Navigator(
    private val mBackStack: NavBackStack<NavKey>,
) {
    fun navigate(route: NavKey) {
        if (mBackStack.lastOrNull()?.let { it::class == route::class } != true) {
            mBackStack.add(route)
        }
    }

    fun goBack() {
        if (mBackStack.size > 1) {
            mBackStack.removeLastOrNull()
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
