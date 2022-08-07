package com.xayah.databackup.fragment.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.activity.guide.GuideViewModel
import com.xayah.databackup.databinding.FragmentGuideOneBinding


class GuideOneFragment : Fragment() {
    private var _binding: FragmentGuideOneBinding? = null
    private val binding get() = _binding!!
    private lateinit var guideViewModel: GuideViewModel
    private lateinit var viewModel: GuideOneViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideOneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        guideViewModel = ViewModelProvider(requireActivity())[GuideViewModel::class.java]
        viewModel = ViewModelProvider(this)[GuideOneViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        guideViewModel.apply {
            btnNextOnClick.postValue {
                guideViewModel.navigation.postValue(R.id.action_guideOneFragment_to_guideUpdateFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}