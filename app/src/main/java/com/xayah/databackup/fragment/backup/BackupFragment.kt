package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Room

class BackupFragment : Fragment() {
    lateinit var viewModel: BackupViewModel

    private var room: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[BackupViewModel::class.java]
        viewModel.binding?.viewModel = viewModel
        viewModel.binding = FragmentBackupBinding.inflate(inflater, container, false)
        return viewModel.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    private fun initialize() {
        val mContext = requireActivity()
        room = Room(mContext)
        viewModel.initialize(mContext, room) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.binding = null
        room?.close()
        room = null
        if (!viewModel.isProcessing)
            viewModel.clear()
    }
}