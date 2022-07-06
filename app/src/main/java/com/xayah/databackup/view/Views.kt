package com.xayah.databackup.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.R
import androidx.appcompat.widget.ListPopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.xayah.databackup.view.adapter.PopupListAdapter
import com.xayah.databackup.view.util.dp

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

fun PopupListAdapter.measureWidth(context: Context): Int {
    val parent = FrameLayout(context)

    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    var itemView: View? = null
    var maxWidth = 0
    var itemType = 0

    for (i in 0 until count) {
        val positionType = getItemViewType(i)
        if (positionType != itemType) {
            itemType = positionType
            itemView = null
        }

        itemView = getView(i, itemView, parent)
        itemView.measure(widthMeasureSpec, heightMeasureSpec)

        val itemWidth: Int = itemView.measuredWidth

        if (itemWidth > maxWidth) {
            maxWidth = itemWidth
        }
    }

    return maxWidth
}

fun InputChip(layoutInflater: LayoutInflater, chipGroup: ChipGroup): Chip {
    return layoutInflater.inflate(
        com.xayah.databackup.R.layout.input_chip,
        chipGroup,
        false
    ) as Chip
}

fun ListPopupWindow.fastInitialize(v: View, items: Array<String>, choice: Int) {
    this.apply {
        val context = v.context
        val adapter = PopupListAdapter(
            context,
            items.toList(),
            choice,
        )
        setAdapter(adapter)
        anchorView = v
        width = adapter.measureWidth(context).coerceAtLeast(150.dp)
        isModal = true
        horizontalOffset = 65.dp
    }
}