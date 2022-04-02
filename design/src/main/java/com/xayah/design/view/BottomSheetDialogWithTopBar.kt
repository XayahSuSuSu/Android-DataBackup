package com.xayah.design.view

import android.graphics.drawable.TransitionDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RawRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.xayah.design.R
import com.xayah.design.util.dp


fun BottomSheetDialog.setWithTopBar(): LinearLayout {
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
            gravity = Gravity.CENTER
            setPadding(0, 16.dp, 0, 16.dp)
            addView(topBar)
        }
        setContentView(mainView)
        show()
        return mainView
    }
}

fun BottomSheetDialog.setWithTopBarAndTips(
    title: String,
    tips: String,
    @RawRes lottie: Int,
    onConfirm: () -> Unit
) {
    this.apply {
        val that = this
        val titleView =
            MaterialTextView(context).apply {
                layoutParams = LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = 16.dp
                }
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium)
                text = title
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val lottieAnimationView = LottieAnimationView(this.context).apply {
            layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200.dp
                ).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            setAnimation(lottie)
            playAnimation()
            repeatCount = LottieDrawable.INFINITE
        }
        val tipsView =
            MaterialTextView(context).apply {
                layoutParams = LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = 16.dp
                }
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                text = tips
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val materialButton = MaterialButton(this.context).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16.dp
                }
            text = this.context.getString(R.string.dialog_positive)
            setOnClickListener { that.dismiss(); onConfirm() }
        }
        setWithTopBar().apply {
            addView(titleView)
            addView(lottieAnimationView)
            addView(tipsView)
            addView(materialButton)
        }
    }
}
