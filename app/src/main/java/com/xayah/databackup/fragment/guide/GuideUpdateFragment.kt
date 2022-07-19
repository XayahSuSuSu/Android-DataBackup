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
import com.xayah.databackup.databinding.FragmentGuideUpdateBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.readInitializedVersionName
import com.xayah.databackup.util.saveInitializedVersionName


class GuideUpdateFragment : Fragment() {
    private var _binding: FragmentGuideUpdateBinding? = null
    private val binding get() = _binding!!
    private lateinit var hostActivity: GuideActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this)[GuideUpdateViewModel::class.java]
        binding.viewModel = viewModel

        hostActivity = requireActivity() as GuideActivity
        hostActivity.setBtnOnClickListener {
            nextStep()
        }

        viewModel.initialize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun nextStep() {
        if (hostActivity.readInitializedVersionName().isNotEmpty()) {
            hostActivity.saveInitializedVersionName(App.versionName)
            App.initializeGlobalList()
            hostActivity.toMainActivity()
        } else {
            hostActivity.navigate(R.id.action_guideUpdateFragment_to_guideTwoFragment)
            hostActivity.setBtnText(GlobalString.grantRootAccess)
        }
    }
}