package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AnimationUtils.loadAnimation
import android.view.animation.LayoutAnimationController
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.dp
import kotlinx.coroutines.*


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
        val linearProgressIndicator = LinearProgressIndicator(requireContext()).apply {
            layoutParams =
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    .apply {
                        marginStart = 100.dp
                        marginEnd = 100.dp
                    }
            trackCornerRadius = 3.dp
            isIndeterminate = true
        }
        binding.linearLayout.addView(linearProgressIndicator)
        val mAdapter = MultiTypeAdapter().apply {
            register(AppListAdapter())
            items = Command.getAppList(requireContext())
        }
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = GridLayoutManager(requireContext(), 1)
            visibility = View.GONE
            layoutAnimation = LayoutAnimationController(
                loadAnimation(
                    context,
                    androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom
                )
            ).apply {
                order = LayoutAnimationController.ORDER_NORMAL
                delay = 0.3F
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                linearProgressIndicator.visibility = View.GONE
                if (_binding != null)
                    binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}