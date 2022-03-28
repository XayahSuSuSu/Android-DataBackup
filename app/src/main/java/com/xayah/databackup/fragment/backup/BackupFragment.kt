package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Command


class BackupFragment : Fragment() {
    private var _binding: FragmentBackupBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = ViewModelProvider(this)[BackupViewModel::class.java]

        initialize()
    }

    private fun initialize() {
        val adapter = MultiTypeAdapter()
        adapter.register(AppListAdapter())
        binding.recyclerView.adapter = adapter
        val layoutManager = GridLayoutManager(requireContext(), 1)
        binding.recyclerView.layoutManager = layoutManager
        adapter.items = Command.getAppList(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}