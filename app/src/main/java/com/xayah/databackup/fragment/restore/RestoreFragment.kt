package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.button.MaterialButton
import com.xayah.databackup.R
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.dp


class RestoreFragment : Fragment() {
    private var _binding: FragmentRestoreBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = ViewModelProvider(this)[RestoreViewModel::class.java]

        initialize()
    }

    private fun initialize() {
        val mContext = requireContext()
        val lottieAnimationView = LottieAnimationView(context)
        lottieAnimationView.apply {
            id = LottieAnimationView.generateViewId()
            layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200.dp
                ).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            setAnimation(R.raw.file)
            playAnimation()
            repeatCount = LottieDrawable.INFINITE
        }
        binding.relativeLayout.addView(lottieAnimationView)
        val materialButton = MaterialButton(mContext).apply {
            layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, lottieAnimationView.id)
                }
            text = mContext.getString(R.string.choose_backup)
        }
        binding.relativeLayout.addView(materialButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}