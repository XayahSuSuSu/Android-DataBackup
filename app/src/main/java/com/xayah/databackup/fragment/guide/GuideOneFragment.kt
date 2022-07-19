package com.xayah.databackup.fragment.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.activity.guide.GuideActivity
import com.xayah.databackup.databinding.FragmentGuideOneBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.readInitializedVersionName


class GuideOneFragment : Fragment() {
    private var _binding: FragmentGuideOneBinding? = null
    private val binding get() = _binding!!
    private lateinit var hostActivity: GuideActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideOneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this)[GuideOneViewModel::class.java]
        binding.viewModel = viewModel

        hostActivity = requireActivity() as GuideActivity
        if (hostActivity.readInitializedVersionName() == App.versionName) {
            App.initializeGlobalList()
            hostActivity.toMainActivity()
        }

        hostActivity.setBtnOnClickListener {
            nextStep()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun nextStep() {
        hostActivity.navigate(R.id.action_guideOneFragment_to_guideTwoFragment)
        hostActivity.setBtnText(GlobalString.grantRootAccess)
    }
}