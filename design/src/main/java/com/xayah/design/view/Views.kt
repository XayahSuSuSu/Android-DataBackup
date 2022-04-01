package com.xayah.design.view

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.RelativeLayout
import androidx.appcompat.R
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.xayah.design.util.dp

fun LinearProgressIndicator.fastInitialize() {
    this.apply {
        layoutParams =
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
                .apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                    marginStart = 100.dp
                    marginEnd = 100.dp
                }
        trackCornerRadius = 3.dp
        isIndeterminate = true
    }
}

fun RecyclerView.fastInitialize() {
    this.apply {
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        layoutManager = GridLayoutManager(this.context, 1)
        layoutAnimation = LayoutAnimationController(
            AnimationUtils.loadAnimation(
                context,
                R.anim.abc_grow_fade_in_from_bottom
            )
        ).apply {
            order = LayoutAnimationController.ORDER_NORMAL
            delay = 0.3F
        }
    }
}

@SuppressLint("NotifyDataSetChanged")
fun RecyclerView.notifyDataSetChanged() {
    this.apply {
        adapter?.notifyDataSetChanged()
        startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.abc_grow_fade_in_from_bottom
            )
        )
    }
}

