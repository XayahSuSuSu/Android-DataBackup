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
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.databinding.ActivitySelectBinding
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectActivity : AppCompatActivity() {
    lateinit var mContext: Context
    lateinit var binding: ActivitySelectBinding
    lateinit var adapter: AppListAdapter
    lateinit var mShell: Shell
    lateinit var menuSave: MenuItem
    lateinit var menuRefresh: MenuItem
    lateinit var menuConsole: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        mContext = this
        binding()
        init()
    }

    override fun onBackPressed() {
        if (adapter.isChanged) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_query_tips))
                .setMessage(getString(R.string.dialog_query_save))
                .setNegativeButton(getString(R.string.dialog_query_no)) { _, _ ->
                    if (!menuSave.isVisible)
                        mShell.close()
                    finish()
                }
                .setPositiveButton(getString(R.string.dialog_query_yes)) { _, _ ->
                    if (!menuSave.isVisible) {
                        mShell.close()
                    } else {
                        mShell.saveAppList(adapter.appList)
                        Toast.makeText(
                            mContext,
                            getString(R.string.dialog_query_save_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finish()
                }
                .setNeutralButton(getString(R.string.dialog_query_cancel)) { _, _ -> }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1) {
            adapter.notifyDataSetChanged()
            init()
        }
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_select)
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
                R.id.menu_save -> {
                    if (adapter.appList.isEmpty()) {
                        Snackbar.make(
                            binding.coordinatorLayout,
                            getString(R.string.snackbar_no_apps),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        adapter.isChanged = false
                        mShell.saveAppList(adapter.appList)
                        Snackbar.make(
                            binding.coordinatorLayout,
                            getString(R.string.dialog_query_save_successfully),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    }
                    true
                }
                R.id.menu_refresh -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_query_tips))
                        .setMessage(getString(R.string.dialog_query_app_list))
                        .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
                        .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ ->
                            adapter.appList = mutableListOf()
//                            mShell.onGenerateAppList()

                            TransitionUtil.TransitionX(window.decorView as ViewGroup)
                            menuSave.isVisible = false
                            menuRefresh.isVisible = false

                            binding.content.visibility = View.GONE
                            binding.progress.isVisible = true
                            binding.empty.isVisible = false
                            binding.console.logs = ""

                            mShell.generateAppList({
                                val content = it.replace("\u001B[0m", "").replace("  -", " -")
                                    .replace("(.*?)m -".toRegex(), " -") + "\n"
                                binding.progress.textViewProgress.text = content

                                binding.console.logs += content
                                runOnUiThread {
                                    binding.nestedScrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            }, {
                                if (it == true) {
                                    adapter.notifyDataSetChanged()
                                    binding.topAppBar.title = getString(R.string.title_select_apps)
                                    binding.console.isVisible = false
                                    binding.linearLayoutMain.visibility = View.VISIBLE
                                    init()
                                }
                            })
                            adapter.isChanged = true

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
                else -> false
            }
        }
    }

    private fun init() {
        setResult(2, intent)
        adapter = AppListAdapter(this)
        mShell = Shell(this)
        val menu = binding.topAppBar.menu
        menuSave = menu.findItem(R.id.menu_save)
        menuRefresh = menu.findItem(R.id.menu_refresh)
        menuConsole = menu.findItem(R.id.menu_console)
        TransitionUtil.TransitionX(window.decorView as ViewGroup)
        binding.progress.isVisible = true
        menuSave.isVisible = true
        menuRefresh.isVisible = true
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAppList.layoutManager = layoutManager
        GlobalScope.launch {
            val appPackages = ShellUtil.getAppPackages(mShell.APP_LIST_FILE_PATH)
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