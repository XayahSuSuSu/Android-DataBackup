package com.xayah.databackup.view.util

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes

val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

fun Context.getPixels(@DimenRes resId: Int): Int {
    return resources.getDimensionPixelSize(resId)
}

fun Context.resolveThemedColor(@AttrRes resId: Int): Int {
    return TypedValue().apply {
        theme.resolveAttribute(resId, this, true)
    }.data
}

fun Context.resolveThemedBoolean(@AttrRes resId: Int): Boolean {
    return TypedValue().apply {
        theme.resolveAttribute(resId, this, true)
    }.data != 0
}

fun Context.resolveThemedResourceId(@AttrRes resId: Int): Int {
    return TypedValue().apply {
        theme.resolveAttribute(resId, this, true)
    }.resourceId
}
