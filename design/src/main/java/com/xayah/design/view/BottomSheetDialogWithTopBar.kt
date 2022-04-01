package com.xayah.design.view

import android.graphics.drawable.TransitionDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xayah.design.R
import com.xayah.design.util.dp


fun BottomSheetDialog.setWithTopBar(childView: View?) {
    this.apply {
        val context = this.context
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        val topBar: View = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(36.dp, 4.dp).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = TransitionDrawable(
                arrayOf(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.bottom_sheet_dialog_topbar
                    ),
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.bottom_sheet_dialog_topbar_activated
                    )
                )
            )
        }
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        (topBar.background as TransitionDrawable).startTransition(150)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        (topBar.background as TransitionDrawable).reverseTransition(150)
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        (topBar.background as TransitionDrawable).reverseTransition(150)
                    }
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        val mainView = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16.dp, 0, 16.dp)
            addView(topBar)
            addView(childView)
        }
        setContentView(mainView)
        show()
    }
}

