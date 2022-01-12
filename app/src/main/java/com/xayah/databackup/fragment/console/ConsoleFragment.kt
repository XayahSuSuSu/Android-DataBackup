package com.xayah.databackup.fragment.console

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.databinding.FragmentConsoleBinding

class ConsoleFragment : Fragment() {

    companion object {
        fun newInstance() = ConsoleFragment()
    }

    private lateinit var binding: FragmentConsoleBinding

    private lateinit var viewModel: ConsoleViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ConsoleViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.initialize(binding.terminalView, requireActivity())
        binding.terminalView.setTerminalViewClient(viewModel.mTermuxTerminalViewClient)
        binding.terminalView.setBackgroundColor(Color.BLACK)
        binding.terminalView.setTextSize(30)
        binding.terminalView.setTypeface(Typeface.MONOSPACE)
        binding.terminalView.attachSession(viewModel.session)
    }

}