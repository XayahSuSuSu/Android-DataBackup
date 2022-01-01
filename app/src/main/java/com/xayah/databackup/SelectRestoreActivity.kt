package com.xayah.databackup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.databinding.ActivityRestoreSelectBinding
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectRestoreActivity : AppCompatActivity() {
    lateinit var mContext: Context
    lateinit var binding: ActivityRestoreSelectBinding
    lateinit var adapter: AppListAdapter
    lateinit var mShell: Shell
    lateinit var menuCheck: MenuItem
    lateinit var menuConsole: MenuItem
    lateinit var menuSearch: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        mContext = this
        binding()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_restore_select)
        binding.chipBackup.text = getString(R.string.select_all_restore)
        binding.chipOnlyApp.setOnCheckedChangeListener { _, isChecked ->
            adapter.isChanged = true
            if (isChecked)
                adapter.selectAll(0, 1)
            else
                adapter.selectAll(0, 0)
        }
        binding.chipBackup.setOnCheckedChangeListener { _, isChecked ->
            adapter.isChanged = true
            if (isChecked)
                adapter.selectAll(1, 1)
            else
                adapter.selectAll(1, 0)
        }
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.title = getString(R.string.title_select_apps)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_check -> {
                    mShell.saveBackupAppList(intent.getStringExtra("path")!!, adapter.appList)
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_query_tips))
                        .setMessage(getString(R.string.dialog_query_restore))
                        .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
                        .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ ->
                            TransitionUtil.TransitionX(window.decorView as ViewGroup)
                            menuCheck.isVisible = false
                            menuSearch.isVisible = false

                            binding.content.visibility = View.GONE
                            binding.progress.isVisible = true
                            binding.empty.isVisible = false
                            binding.console.logs = ""

                            mShell.restore(intent.getStringExtra("path")!!, {
                                val content = it.replace("\u001B[0m", "").replace("  -", " -")
                                    .replace("(.*?)m -".toRegex(), " -") + "\n"
                                binding.progress.textViewProgress.text = content

                                binding.console.logs += content
                                runOnUiThread {
                                    binding.nestedScrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            }, {
                                if (it == true) {
                                    binding.topAppBar.title = getString(R.string.title_select_apps)
                                    binding.console.isVisible = false
                                    binding.linearLayoutMain.visibility = View.VISIBLE
                                    Toast.makeText(this, getString(R.string.toast_restore_successfully), Toast.LENGTH_SHORT).show()
                                    init()
                                }
                            })
                        }
                        .show()
                    true
                }
                R.id.menu_console -> {
                    TransitionUtil.TransitionX(window.decorView as ViewGroup)
                    if (!binding.console.isVisible) {
                        binding.topAppBar.title = getString(R.string.title_console)
                        binding.console.isVisible = true
                        binding.linearLayoutMain.visibility = View.GONE
                    } else {
                        binding.topAppBar.title = getString(R.string.title_select_apps)
                        binding.console.isVisible = false
                        binding.linearLayoutMain.visibility = View.VISIBLE
                    }
                    true
                }
                R.id.menu_search -> {
                    val searchView = menuItem.actionView as SearchView
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(string: String): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(string: String): Boolean {
                            adapter.filter.filter(string)
                            return false
                        }
                    })
                    true
                }
                else -> false
            }
        }
    }

    private fun init() {
        setResult(2, intent)
        adapter = AppListAdapter(this)
        mShell = Shell(this)
        val menu = binding.topAppBar.menu
        menuCheck = menu.findItem(R.id.menu_check)
        menuConsole = menu.findItem(R.id.menu_console)
        menuSearch = menu.findItem(R.id.menu_search)
        binding.progress.isVisible = true
        menuCheck.isVisible = true
        menuSearch.isVisible = true
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAppList.layoutManager = layoutManager
        GlobalScope.launch {
            val appPackages =
                ShellUtil.getAppPackages("${intent.getStringExtra("path")}/${mShell.APP_LIST_FILE_NAME}")
            for (i in appPackages) {
                val app = i.substring(0, i.lastIndexOf(" "))
                val packageName = i.substring(i.lastIndexOf(" ") + 1)
                try {
                    val (appIcon, appName, appPackage) = DataUtil.getAppInfo(mContext, packageName)
                    val appInfo = AppInfo(
                        appIcon,
                        appName,
                        appPackage,
                        app.contains("#"),
                        app.contains("!")
                    )
                    adapter.addApp(appInfo)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            runOnUiThread {
                binding.recyclerViewAppList.adapter = adapter
                var onlyAppAll = true
                var backupAll = true
                for (i in adapter.appList) {
                    if (!i.onlyApp)
                        onlyAppAll = false
                    if (i.ban)
                        backupAll = false
                }
                TransitionUtil.TransitionX(window.decorView as ViewGroup)
                binding.progress.isVisible = false
                if (adapter.appList.isEmpty()) {
                    binding.content.visibility = View.GONE
                    binding.empty.isVisible = true
                } else {
                    binding.content.visibility = View.VISIBLE
                    binding.empty.isVisible = false
                }
                binding.chipOnlyApp.isChecked = onlyAppAll
                binding.chipBackup.isChecked = backupAll
                adapter.isChanged = false
            }
        }
    }
}