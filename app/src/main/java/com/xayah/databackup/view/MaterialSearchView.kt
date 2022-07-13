package com.xayah.databackup.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.appcompat.widget.SearchView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel


class MaterialSearchView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.searchViewStyle
) : SearchView(context, attributeSet, defStyleAttr) {
    init {
        val view = this
        val shapeAppearanceModel = ShapeAppearanceModel.builder(
            context,
            R.style.ShapeAppearance_MaterialComponents_SmallComponent,
            R.style.ShapeAppearanceOverlay_Material3_Button
        ).build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
            fillColor =
                ColorStateList.valueOf(MaterialColors.getColor(view, R.attr.colorSurfaceVariant))
        }
        background = shapeDrawable
        findViewById<View>(R.id.search_plate).background = null
        findViewById<View>(R.id.submit_area).background = null
    }
}