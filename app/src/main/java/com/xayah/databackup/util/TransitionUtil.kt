package com.xayah.databackup.util

import android.view.ViewGroup
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialSharedAxis

class TransitionUtil {
    companion object {
        fun TransitionX(viewGroup: ViewGroup) {
            val materialSharedAxis = MaterialSharedAxis(MaterialSharedAxis.X, true)
            TransitionManager.beginDelayedTransition(
                viewGroup,
                materialSharedAxis
            )
        }
    }
}