package com.xayah.databackup.activity.backup.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoBase
import com.xayah.databackup.databinding.ActivityBackupAppListBinding
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged

class BackupAppListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupAppListBinding
    private lateinit var viewModel: BackupAppListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityBackupAppListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[BackupAppListViewModel::class.java]
        binding.viewModel = viewModel
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.apply {
            adapter = viewModel.mAdapter
            fastInitialize()
        }
        viewModel.initialize {
            binding.recyclerView.notifyDataSetChanged()
            binding.recyclerView.visibility = View.VISIBLE
            binding.lottieAnimationView.visibility = View.GONE
        }
    }

    private fun saveAppList() {
        val appInfoBackupList = mutableListOf<AppInfoBackup>()
        for (i in viewModel.mAppInfoBackupList) appInfoBackupList.add(i)
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(appInfoBackupList as MutableList<Any>),
            Path.getBackupAppListPath()
        )
    }

    override fun onPause() {
        super.onPause()
        saveAppList()
    }
}