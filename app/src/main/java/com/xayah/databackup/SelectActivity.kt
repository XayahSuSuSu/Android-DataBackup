package com.xayah.databackup

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.databinding.ActivitySelectBinding
import com.xayah.databackup.util.DataUtil
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectActivity : AppCompatActivity() {
    lateinit var mContext: Context
    lateinit var binding: ActivitySelectBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        setTitle(R.string.title_SelectApps)
        mContext = this

        binding()
        init()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_select)
    }

    private fun init() {
        val adapter = AppListAdapter(this)
        val mShell = ShellUtil(this)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAppList.layoutManager = layoutManager
        GlobalScope.launch {
            val appPackages = mShell.getAppPackages()
            for (i in appPackages) {
                val app = i.split(" ")[0]
                val packageName = i.split(" ")[1]
                val (appIcon, appName, appPackage) = DataUtil.getAppInfo(mContext, packageName)
                val appInfo = AppInfo(
                    appIcon,
                    appName,
                    appPackage,
                    app.contains("#"),
                    app.contains("!")
                )
                adapter.addApp(appInfo)
            }
            runOnUiThread {
                binding.recyclerViewAppList.adapter = adapter
            }
        }
    }
}