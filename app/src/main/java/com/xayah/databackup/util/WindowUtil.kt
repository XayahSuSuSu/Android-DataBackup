package com.xayah.databackup.util

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController

class WindowUtil {
    companion object {
        fun setWindowMode(isLight: Boolean, window: Window) {
            if (isLight) {
                if (Build.VERSION.SDK_INT >= 30) {
                    window.decorView.windowInsetsController?.apply {
                        setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    }
                } else {
                    window.decorView.systemUiVisibility =
                        window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            } else {
                if (Build.VERSION.SDK_INT >= 30) {
                    window.decorView.windowInsetsController?.apply {
                        setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    }
                } else {
                    window.decorView.systemUiVisibility =
                        window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }
}