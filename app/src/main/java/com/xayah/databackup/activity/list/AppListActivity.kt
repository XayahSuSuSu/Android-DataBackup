package com.xayah.databackup.activity.list

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.activity.AppCompatActivityBase
import com.xayah.databackup.databinding.ActivityAppListBinding
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.notifyDataSetChanged

class AppListActivity : AppCompatActivityBase() {
    private lateinit var binding: ActivityAppListBinding
    private lateinit var viewModel: AppListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityAppListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AppListViewModel::class.java]
        binding.viewModel = viewModel
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.apply {
            adapter = viewModel.mAdapter
            fastInitialize()
        }
        viewModel.initialize(intent.getBooleanExtra("isRestore", false)) {
            binding.recyclerView.notifyDataSetChanged()
            binding.recyclerView.visibility = View.VISIBLE
            binding.lottieAnimationView.visibility = View.GONE
        }
    }
}