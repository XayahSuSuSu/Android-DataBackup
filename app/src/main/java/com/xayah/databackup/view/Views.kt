package com.xayah.databackup.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.R
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.ListPopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.xayah.databackup.util.GlobalString
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

fun RecyclerView.fastInitialize(isHorizontal: Boolean = false) {
    this.apply {
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        layoutManager = GridLayoutManager(this.context, 1).apply {
            if (isHorizontal) orientation = LinearLayoutManager.HORIZONTAL
        }
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
                        com.xayah.databackup.R.drawable.bottom_sheet_dialog_topbar
                    ),
                    AppCompatResources.getDrawable(
                        context,
                        com.xayah.databackup.R.drawable.bottom_sheet_dialog_topbar_activated
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

fun BottomSheetDialog.setLoading() {
    this.apply {
        val titleView =
            MaterialTextView(context).apply {
                layoutParams = LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = 16.dp
                }
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium)
                text = GlobalString.pleaseWait
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
            setAnimation(com.xayah.databackup.R.raw.loading)
            playAnimation()
            repeatCount = LottieDrawable.INFINITE
        }
        setWithTopBar().apply {
            addView(titleView)
            addView(lottieAnimationView)
        }
        setCancelable(false)
    }
}
