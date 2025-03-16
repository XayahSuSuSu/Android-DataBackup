package com.xayah.databackup.util

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
private val NavBackStackEntry.isLifecycleResumed get() = this.lifecycle.currentState == Lifecycle.State.RESUMED

fun <T : Any> NavHostController.navigateSafely(route: T) {
    if (currentBackStackEntry?.isLifecycleResumed == true) {
        navigate(route) { launchSingleTop = true }
    }
}

fun NavHostController.popBackStackSafely() {
    if (currentBackStackEntry?.isLifecycleResumed == true) {
        popBackStack()
    }
}
