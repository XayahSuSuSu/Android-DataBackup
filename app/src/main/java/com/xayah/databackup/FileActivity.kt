package com.xayah.databackup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.adapter.FileListAdapter
import com.xayah.databackup.databinding.ActivityFileBinding
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean

class FileActivity : AppCompatActivity() {
    lateinit var binding: ActivityFileBinding
    lateinit var adapter: FileListAdapter
//    lateinit var mShell: Shell

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        binding()
        init()
    }

    override fun onBackPressed() {
        if (adapter.pathToString() == "") {
            super.onBackPressed()
        } else {
            adapter.path.removeLast()
            val fileList = ShellUtil.getFile(adapter.pathToString())
            adapter.fileList = fileList
            binding.topAppBar.subtitle = adapter.pathToString()
            adapter.notifyDataSetChanged()
        }
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_file)
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.title = getString(R.string.title_choose_backup_dir)
        adapter = FileListAdapter(this)
        adapter.bind(binding)
        adapter.init()
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
//                R.id.menu_choose -> {
//                    if (mShell.checkRestoreScript("${adapter.pathToString()}/")) {
//                        MaterialAlertDialogBuilder(this)
//                            .setTitle(getString(R.string.dialog_query_tips))
//                            .setMessage(getString(R.string.dialog_query_choose_dir))
//                            .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
//                            .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ ->
//                                val intent =
//                                    Intent().apply { putExtra("path", adapter.pathToString()) }
//                                setResult(Activity.RESULT_OK, intent)
//                                finish()
//                            }
//                            .show()
//                    } else {
//                        MaterialAlertDialogBuilder(this)
//                            .setTitle(getString(R.string.dialog_query_tips))
//                            .setMessage(getString(R.string.dialog_query_choose_right_dir))
//                            .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ -> }
//                            .show()
//                    }
//                }
            }
            true
        }
    }

    private fun init() {
//        mShell = Shell(this)
    }
}