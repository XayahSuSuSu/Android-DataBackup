package com.xayah.databackup.fragment.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.activity.guide.GuideViewModel
import com.xayah.databackup.databinding.FragmentGuideUpdateBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.readInitializedVersionName
import com.xayah.databackup.util.saveInitializedVersionName
import kotlinx.coroutines.launch


class GuideUpdateFragment : Fragment() {
    private var _binding: FragmentGuideUpdateBinding? = null
    private val binding get() = _binding!!
    private lateinit var guideViewModel: GuideViewModel
    private lateinit var viewModel: GuideUpdateViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        guideViewModel = ViewModelProvider(requireActivity())[GuideViewModel::class.java]
        viewModel = ViewModelProvider(this)[GuideUpdateViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        guideViewModel.apply {
            btnPrevText.value = GlobalString.cancel
            btnNextText.value = GlobalString.nextStep
            btnNextOnClick.postValue {
                nextStep()
            }
        }
        viewModel.initialize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun nextStep() {
        viewModel.viewModelScope.launch {
            if (App.globalContext.readInitializedVersionName().isNotEmpty()) {
                App.globalContext.saveInitializedVersionName(App.versionName)
                guideViewModel.finishAndEnter.postValue(true)
            } else {
                guideViewModel.navigation.postValue(R.id.action_guideUpdateFragment_to_guideEnvFragment)
                guideViewModel.btnNextText.postValue(GlobalString.grantRootAccess)
            }
        }
    }
}