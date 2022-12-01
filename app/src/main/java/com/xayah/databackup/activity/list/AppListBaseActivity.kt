package com.xayah.databackup.activity.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.data.AppListSort
import com.xayah.databackup.data.AppListType
import com.xayah.databackup.databinding.ActivityAppListBinding
import com.xayah.databackup.view.fastInitialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AppListPreferences(
    var type: AppListType = AppListType.InstalledApp,
    var sort: AppListSort = AppListSort.Alphabet,
)

abstract class AppListBaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppListBinding
    private lateinit var viewModel: AppListBaseViewModel

    companion object {
        const val TAG = "AppListBaseActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityAppListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AppListBaseViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.bottomAppBar)

        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.recyclerView.apply {
            fastInitialize()
            adapter = viewModel.mAdapter
        }

        viewModel.pref = AppListPreferences()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        viewModel.pref.type = AppListType.InstalledApp
                        initialize(viewModel.pref)
                    }
                    1 -> {
                        viewModel.pref.type = AppListType.SystemApp
                        initialize(viewModel.pref)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        initialize(viewModel.pref)
    }

    private fun initialize(pref: AppListPreferences) {
        CoroutineScope(Dispatchers.Main).launch {
            // 开始加载
            binding.circularProgressIndicator.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.recyclerView.scrollToPosition(0)

            loadList(pref)
            viewModel.mAdapter.apply {
                onAdapterRegister(this)
                items = onAdapterListAdd(pref)
                notifyDataSetChanged()
            }

            // 结束加载
            binding.circularProgressIndicator.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    abstract fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter)

    abstract fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any>

    abstract suspend fun loadList(pref: AppListPreferences)
}
