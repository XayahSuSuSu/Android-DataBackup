package com.xayah.databackup.activity.list

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.R
import com.xayah.databackup.data.AppListFilter
import com.xayah.databackup.data.AppListSelection
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
    var searchKeyWord: String = "",
    var installedAppSelection: AppListSelection = AppListSelection.None,
    var systemAppSelection: AppListSelection = AppListSelection.None,
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
                            AppListSort.DataSizeAscending, AppListSort.DataSizeDescending -> {
                                // 数据大小
                                materialButtonToggleGroup.check(R.id.button_data_size)
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
                        // 数据大小
                        buttonDataSize.apply {
                            val dataSizeAscendingText =
                                "${GlobalString.dataSize} ${GlobalString.symbolTriangle}"
                            val dataSizeDescendingText =
                                "${GlobalString.dataSize} ${GlobalString.symbolAntiTriangle}"
                            if (viewModel.pref.sort == AppListSort.DataSizeAscending) {
                                // 升序
                                buttonDataSize.text = dataSizeAscendingText
                            } else {
                                // 降序
                                buttonDataSize.text = dataSizeDescendingText
                            }
                            setOnClickListener {
                                if (viewModel.pref.sort == AppListSort.DataSizeAscending) {
                                    viewModel.pref.sort = AppListSort.DataSizeDescending
                                    text = dataSizeDescendingText
                                } else {
                                    viewModel.pref.sort = AppListSort.DataSizeAscending
                                    text = dataSizeAscendingText
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
            R.id.menu_app -> {
                when (viewModel.pref.type) {
                    AppListType.InstalledApp -> {
                        if (viewModel.pref.installedAppSelection == AppListSelection.App) {
                            viewModel.pref.installedAppSelection = AppListSelection.AppReverse
                        } else {
                            viewModel.pref.installedAppSelection = AppListSelection.App
                        }
                    }
                    AppListType.SystemApp -> {
                        if (viewModel.pref.systemAppSelection == AppListSelection.App) {
                            viewModel.pref.systemAppSelection = AppListSelection.AppReverse
                        } else {
                            viewModel.pref.systemAppSelection = AppListSelection.App
                        }
                    }
                }
                initialize(viewModel.pref)
                true
            }
            R.id.menu_all -> {
                when (viewModel.pref.type) {
                    AppListType.InstalledApp -> {
                        if (viewModel.pref.installedAppSelection == AppListSelection.All) {
                            viewModel.pref.installedAppSelection = AppListSelection.AllReverse
                        } else {
                            viewModel.pref.installedAppSelection = AppListSelection.All
                        }
                    }
                    AppListType.SystemApp -> {
                        if (viewModel.pref.systemAppSelection == AppListSelection.All) {
                            viewModel.pref.systemAppSelection = AppListSelection.AllReverse
                        } else {
                            viewModel.pref.systemAppSelection = AppListSelection.All
                        }
                    }
                }
                initialize(viewModel.pref)
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
            binding.searchBar.visibility = View.GONE
            binding.tabLayout.visibility = View.GONE
            binding.bottomAppBar.visibility = View.GONE
            binding.floatingActionButton.visibility = View.GONE
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
            binding.searchBar.visibility = View.VISIBLE
            binding.tabLayout.visibility = View.VISIBLE
            binding.bottomAppBar.visibility = View.VISIBLE
            binding.floatingActionButton.visibility = View.VISIBLE
        }
    }

    private fun bind() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.searchView.editText.setOnEditorActionListener { _, _, event ->
            if (event == null || event.action != KeyEvent.ACTION_DOWN) {
                // 防止触发两次
                return@setOnEditorActionListener false
            }
            runOnMainCoroutine {
                val text = binding.searchView.text
                viewModel.pref.searchKeyWord = text.toString()
                initialize(viewModel.pref)
                binding.searchBar.text = text
                binding.searchView.hide()
            }
            false
        }

        binding.recyclerView.apply {
            fastInitialize()
            adapter = viewModel.mAdapter
        }

        binding.floatingActionButton.setOnClickListener {
            onFloatingActionButtonClick {}
            finish()
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
