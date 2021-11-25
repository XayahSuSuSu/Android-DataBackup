package com.xayah.databackup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.databinding.ActivitySelectBinding
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.DataUtil
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectActivity : AppCompatActivity() {
    lateinit var mContext: Context
    lateinit var binding: ActivitySelectBinding
    lateinit var adapter: AppListAdapter
    lateinit var mShell: ShellUtil
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        setTitle(R.string.title_SelectApps)
        mContext = this

        binding()
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.select_apps_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                mShell.saveAppList(adapter.appList)
            }
            R.id.menu_refresh -> {
                adapter.appList = mutableListOf()
                mShell.onGenerateAppList()
            }
            R.id.menu_backup_only_app -> {
                item.isChecked = !item.isChecked
                GlobalScope.launch {
                    for (i in adapter.appList.indices) {
                        adapter.reverseOnlyApp(i)
                    }
                }
            }
            R.id.menu_backup_all -> {
                item.isChecked = !item.isChecked
                GlobalScope.launch {
                    for (i in adapter.appList.indices) {
                        adapter.reverseBan(i)
                    }
                }
            }
        }
        return false
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
    }

    private fun init() {
        adapter = AppListAdapter(this)
        mShell = ShellUtil(this)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAppList.layoutManager = layoutManager
        GlobalScope.launch {
            val appPackages = mShell.getAppPackages()
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
            }
        }
    }
}