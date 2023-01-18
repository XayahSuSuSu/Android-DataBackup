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
import com.xayah.databackup.util.appReleaseList
import io.noties.markwon.Markwon
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

        // 标题
        guideViewModel.title.set(GlobalString.currentUpdate)

        guideViewModel.apply {
            btnPrevText.value = GlobalString.cancel
            btnNextText.value = GlobalString.nextStep
            btnNextOnClick.postValue {
                nextStep()
            }
        }
        val markwon = Markwon.create(requireContext())
        viewModel.apply {
            runOnScope {
                subtitle.emit("${GlobalString.currentVersion}: ${App.versionName}")
                App.server.releases({ releaseList ->
                    runOnScope {
                        val mReleaseList = releaseList.appReleaseList()
                        if (mReleaseList.isEmpty()) {
                            content.emit(GlobalString.fetchFailed)
                        } else {
                            var info = ""
                            for (i in mReleaseList) {
                                info += "## ${i.name}\n\n"
                                info += "${i.body}\n\n"
                            }
                            content.emit(info)
                        }
                        if (_binding != null)
                            markwon.setMarkdown(binding.content, content.value)
                    }
                }, {
                    runOnScope {
                        content.emit(GlobalString.fetchFailed)
                        if (_binding != null)
                            markwon.setMarkdown(binding.content, content.value)
                    }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun nextStep() {
        viewModel.viewModelScope.launch {
            guideViewModel.navigation.postValue(R.id.action_guideUpdateFragment_to_guideEnvFragment)
        }
    }
}