package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

class RestoreFragment : Fragment() {
    lateinit var viewModel: RestoreViewModel

    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[RestoreViewModel::class.java]
        viewModel.binding?.viewModel = viewModel

        viewModel.binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return viewModel.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val that = this
        materialYouFileExplorer = MaterialYouFileExplorer().apply {
            initialize(that)
        }
    }

    private fun initialize() {
        val mContext = requireActivity()
        viewModel.initialize(mContext, materialYouFileExplorer) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.backupPath = null
        viewModel.binding = null
    }
}