package com.xayah.databackup.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.databinding.FragmentHomeBinding
import com.xayah.databackup.util.Shell
import com.xayah.databackup.util.ShellUtil


class HomeFragment : Fragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val model = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding.viewModel =model
        val mShell = Shell(requireContext())
        mShell.extractAssets()
        model.scriptVersion= ShellUtil.getScriptVersion(requireContext())
        model.storageSpace= ShellUtil.getStorageSpace(requireContext())
    }
}