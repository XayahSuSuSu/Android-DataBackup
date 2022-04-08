package com.xayah.design.util

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ListAdapter


fun ListAdapter.measureWidth(context: Context): Int {
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
