package com.xayah.databackup.activity.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.databinding.ActivityAppListBinding
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.notifyDataSetChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListActivity : AppCompatActivity() {
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

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            App.saveGlobalList()
        }
    }
}