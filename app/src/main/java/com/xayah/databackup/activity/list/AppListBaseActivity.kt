package com.xayah.databackup.activity.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.R
import com.xayah.databackup.data.AppListFilter
import com.xayah.databackup.data.AppListSort
import com.xayah.databackup.data.AppListType
import com.xayah.databackup.databinding.ActivityAppListBinding
import com.xayah.databackup.databinding.BottomSheetFilterBinding
import com.xayah.databackup.databinding.BottomSheetSortBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.setWithTopBar
import com.xayah.databackup.view.title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AppListPreferences(
    var type: AppListType = AppListType.InstalledApp,
    var sort: AppListSort = AppListSort.AlphabetAscending,
    var filter: AppListFilter = AppListFilter.None,
)

abstract class AppListBaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppListBinding
    private lateinit var viewModel: AppListBaseViewModel

    companion object {
        const val TAG = "AppListBaseActivity"
    }

    /**
     * 注册适配器
     */
    abstract fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter)

    /**
     * 填充适配器数据
     */
    abstract fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any>

    /**
     * 刷新列表
     */
    abstract suspend fun refreshList(pref: AppListPreferences)

    /**
     * 设置Tab
     */
    abstract fun setTabLayout(tabLayout: TabLayout)

    /**
     * 保存列表数据
     */
    abstract suspend fun onSave()

    /**
     * FAB点击事件
     */
    abstract fun onFloatingActionButtonClick(l: () -> Unit)

    /**
     * 协程运行
     */
    private fun <T> runOnMainCoroutine(block: suspend () -> T) {
        CoroutineScope(Dispatchers.Main).launch { block() }
    }

    override fun onPause() {
        super.onPause()
        runOnMainCoroutine {
            onSave()
        }
    }

    override fun onBackPressed() {
        runOnMainCoroutine {
            // 为优化用户体验，不再阻塞保存操作
            onSave()
        }
        super.onBackPressed()
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

        // 配置
        viewModel.pref = AppListPreferences()
        initialize(viewModel.pref)

        // 绑定相关
        bind()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sort -> {
                val bottomSheetSortBinding =
                    BottomSheetSortBinding.inflate(LayoutInflater.from(this), null, false).apply {
                        // 默认选中
                        when (viewModel.pref.sort) {
                            AppListSort.AlphabetAscending, AppListSort.AlphabetDescending -> {
                                // 字母表序
                                materialButtonToggleGroup.check(R.id.button_alphabet)
                            }
                            AppListSort.FirstInstallTimeAscending, AppListSort.FirstInstallTimeDescending -> {
                                // 安装时间
                                materialButtonToggleGroup.check(R.id.button_first_install_time)
                            }
                        }
                        // 字母表序
                        buttonAlphabet.apply {
                            val alphabetAscendingText =
                                "${GlobalString.alphabet} ${GlobalString.symbolTriangle}"
                            val alphabetDescendingText =
                                "${GlobalString.alphabet} ${GlobalString.symbolAntiTriangle}"
                            if (viewModel.pref.sort == AppListSort.AlphabetAscending) {
                                // 升序
                                buttonAlphabet.text = alphabetAscendingText
                            } else {
                                // 降序
                                buttonAlphabet.text = alphabetDescendingText
                            }
                            setOnClickListener {
                                if (viewModel.pref.sort == AppListSort.AlphabetAscending) {
                                    viewModel.pref.sort = AppListSort.AlphabetDescending
                                    text = alphabetDescendingText
                                } else {
                                    viewModel.pref.sort = AppListSort.AlphabetAscending
                                    text = alphabetAscendingText
                                }
                                initialize(viewModel.pref)
                            }
                        }
                        // 安装时间
                        buttonFirstInstallTime.apply {
                            val firstInstallTimeAscendingText =
                                "${GlobalString.installTime} ${GlobalString.symbolTriangle}"
                            val firstInstallTimeDescendingText =
                                "${GlobalString.installTime} ${GlobalString.symbolAntiTriangle}"
                            if (viewModel.pref.sort == AppListSort.FirstInstallTimeAscending) {
                                // 升序
                                buttonFirstInstallTime.text = firstInstallTimeAscendingText
                            } else {
                                // 降序
                                buttonFirstInstallTime.text = firstInstallTimeDescendingText
                            }
                            setOnClickListener {
                                if (viewModel.pref.sort == AppListSort.FirstInstallTimeAscending) {
                                    viewModel.pref.sort = AppListSort.FirstInstallTimeDescending
                                    text = firstInstallTimeDescendingText
                                } else {
                                    viewModel.pref.sort = AppListSort.FirstInstallTimeAscending
                                    text = firstInstallTimeAscendingText
                                }
                                initialize(viewModel.pref)
                            }
                        }
                    }
                BottomSheetDialog(this).apply {
                    setWithTopBar().apply {
                        addView(title(GlobalString.sort))
                        addView(bottomSheetSortBinding.root)
                    }
                }
                true
            }
            R.id.menu_filter -> {
                val bottomSheetFilterBinding =
                    BottomSheetFilterBinding.inflate(LayoutInflater.from(this), null, false).apply {
                        // 默认选中
                        when (viewModel.pref.filter) {
                            AppListFilter.None -> {
                                // 无
                                materialButtonToggleGroup.check(R.id.button_none)
                            }
                            AppListFilter.Selected -> {
                                // 已选择
                                materialButtonToggleGroup.check(R.id.button_selected)
                            }
                            AppListFilter.NotSelected -> {
                                // 未选择
                                materialButtonToggleGroup.check(R.id.button_not_selected)
                            }
                        }
                        buttonNone.setOnClickListener {
                            viewModel.pref.filter = AppListFilter.None
                            initialize(viewModel.pref)
                        }
                        buttonSelected.setOnClickListener {
                            viewModel.pref.filter = AppListFilter.Selected
                            initialize(viewModel.pref)
                        }
                        buttonNotSelected.setOnClickListener {
                            viewModel.pref.filter = AppListFilter.NotSelected
                            initialize(viewModel.pref)
                        }
                    }
                BottomSheetDialog(this).apply {
                    setWithTopBar().apply {
                        addView(title(GlobalString.filter))
                        addView(bottomSheetFilterBinding.root)
                    }
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initialize(pref: AppListPreferences) {
        runOnMainCoroutine {
            // 开始加载
            binding.circularProgressIndicator.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.recyclerView.scrollToPosition(0)

            // 数据填充
            refreshList(pref)
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

    private fun bind() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.recyclerView.apply {
            fastInitialize()
            adapter = viewModel.mAdapter
        }

        binding.floatingActionButton.setOnClickListener {
            onFloatingActionButtonClick {}
        }

        setTabLayout(binding.tabLayout)

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
    }

}
