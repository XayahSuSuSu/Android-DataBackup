package com.xayah.databackup.fragment.backup

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.airbnb.lottie.LottieAnimationView
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.xayah.databackup.App
import com.xayah.databackup.MainActivity
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Room
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.databackup.util.readCompressionType
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged
import com.xayah.design.view.setWithResult
import kotlinx.coroutines.*
import java.text.Collator
import java.util.*

class BackupViewModel : ViewModel() {
    var binding: FragmentBackupBinding? = null

    var isProcessing: Boolean = false

    var appList: MutableList<AppEntity> = mutableListOf()

    var appListAll: MutableList<AppEntity> = mutableListOf()

    var isFiltering = false

    lateinit var mAdapter: MultiTypeAdapter

    var time: Long = 0
    var index = 0
    var total = 0

    var success = 0
    var failed = 0

    fun initialize(context: Context, room: Room?, onInitialized: () -> Unit) {
        val linearProgressIndicator = LinearProgressIndicator(context).apply { fastInitialize() }
        binding?.relativeLayout?.addView(linearProgressIndicator)
        if (!isProcessing) {
            mAdapter = MultiTypeAdapter().apply {
                register(AppListAdapter(room))
                CoroutineScope(Dispatchers.IO).launch {
                    val mAppList = Command.getAppList(context, room).apply {
                        sortWith { appEntity1, appEntity2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appEntity1 as AppEntity).appName)
                                .compareTo(collator.getCollationKey((appEntity2 as AppEntity).appName))
                        }
                    }
                    appList = mAppList
                    appListAll = mAppList
                    items = appList
                    withContext(Dispatchers.Main) {
                        binding?.recyclerView?.notifyDataSetChanged()
                        linearProgressIndicator.visibility = View.GONE
                        binding?.recyclerView?.visibility = View.VISIBLE
                        onInitialized()
                    }
                }
            }
        } else {
            linearProgressIndicator.visibility = View.GONE
            binding?.recyclerView?.visibility = View.VISIBLE
            onInitialized()
        }
        binding?.recyclerView?.apply {
            adapter = mAdapter
            fastInitialize()
        }
    }

    fun setSearchView(context: Context, menu: Menu) {
        val searchView = SearchView(context).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        appList =
                            appListAll.filter { it.appName.contains(newText) }
                                .toMutableList()
                        mAdapter.items = appList
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    return false
                }
            })
            queryHint = this.context.getString(R.string.please_type_key_word)
            isQueryRefinementEnabled = true
        }
        menu.findItem(R.id.backup_search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    isFiltering = true
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    isFiltering = false
                    return true
                }
            })
        }
    }

    fun setReverse(context: FragmentActivity, room: Room?) {
        PopupMenu(context, context.findViewById(R.id.backup_reverse)).apply {
            menuInflater.inflate(R.menu.select, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.select_all_app -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp = true
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.selectAllApp()
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.select_all_data -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupData = true
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.selectAllData()
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.reverse_all_app -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp =
                                !appList[index].backupApp
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.reverseAllApp()
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.reverse_all_data -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupData =
                                !appList[index].backupData
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.reverseAllData()
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                }
                true
            }
            show()
        }
    }

    fun setConfirm(context: FragmentActivity, onCallback: () -> Unit) {
        if (isFiltering) {
            Toast.makeText(
                context,
                context.getString(R.string.please_exit_search_mode),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            MaterialAlertDialogBuilder(context).apply {
                setTitle(context.getString(R.string.tips))
                setCancelable(true)
                setMessage(context.getString(R.string.onConfirm))
                setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                    App.log.clear()
                    time = 0
                    success = 0
                    failed = 0
                    isProcessing = true
                    CoroutineScope(Dispatchers.IO).launch {
                        while (isProcessing) {
                            delay(1000)
                            time += 1
                            val s = String.format("%02d", time % 60)
                            val m = String.format("%02d", time / 60 % 60)
                            val h = String.format("%02d", time / 3600 % 24)
                            withContext(Dispatchers.Main) {
                                (context as MainActivity).binding.toolbar.subtitle = "$h:$m:$s"
                                context.binding.toolbar.title =
                                    "${context.getString(R.string.backup_processing)}: ${index}/${total}"
                            }
                        }
                        withContext(Dispatchers.Main) {
                            (context as MainActivity).binding.toolbar.subtitle =
                                context.viewModel.versionName
                            context.binding.toolbar.title =
                                context.getString(R.string.backup_success)
                        }
                    }
                    binding?.recyclerView?.scrollToPosition(0)
                    onCallback()

                    appList = mutableListOf()
                    appList.addAll(appListAll)
                    mAdapter.items = appList

                    val mAppList = mutableListOf<AppEntity>()
                    mAppList.addAll(appList)
                    for (i in mAppList) {
                        if (!i.backupApp && !i.backupData) {
                            appList.remove(i)
                        } else {
                            appList[appList.indexOf(i)].isProcessing = true
                        }
                    }
                    binding?.recyclerView?.notifyDataSetChanged()
                    mAppList.clear()
                    mAppList.addAll(appList)
                    total = mAppList.size
                    CoroutineScope(Dispatchers.IO).launch {
                        for ((mIndex, i) in mAppList.withIndex()) {
                            App.log.add("----------------------------")
                            App.log.add("${context.getString(R.string.backup_processing)}: ${i.packageName}")
                            var state = true
                            index = mIndex
                            val compressionType = context.readCompressionType()
                            val packageName = i.packageName
                            val outPut = "${context.readBackupSavePath()}/${packageName}"

                            if (appList[0].backupApp) {
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingApp = true
                                    mAdapter.notifyItemChanged(0)
                                    appList[0].progress =
                                        context.getString(R.string.backup_apk_processing)
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.compressAPK(compressionType, packageName, outPut)
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingApp = false
                                    appList[0].backupApp = false
                                    appList[0].progress =
                                        context.getString(R.string.success)
                                    mAdapter.notifyItemChanged(0)
                                }
                            }
                            if (appList[0].backupData) {
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingData = true
                                    appList[0].progress =
                                        "${context.getString(R.string.backup_processing)}user"
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.compress(compressionType, "user", packageName, outPut)
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                withContext(Dispatchers.Main) {
                                    appList[0].progress =
                                        "${context.getString(R.string.backup_processing)}data"
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.compress(compressionType, "data", packageName, outPut)
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                withContext(Dispatchers.Main) {
                                    appList[0].progress =
                                        "${context.getString(R.string.backup_processing)}obb"
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.compress(compressionType, "obb", packageName, outPut)
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                            }
                            withContext(Dispatchers.Main) {
                                appList.removeAt(0)
                                mAdapter.notifyItemRemoved(0)
                            }
                            Command.generateAppInfo(i.appName, i.packageName, outPut).apply {
                                if (!this)
                                    state = false
                            }
                            if (state)
                                success += 1
                            else
                                failed += 1
                        }
                        withContext(Dispatchers.Main) {
                            val showResult = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.backup_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                BottomSheetDialog(context).apply {
                                    setWithResult(
                                        App.log.toString(),
                                        success,
                                        failed
                                    )
                                }
                            }
                            if (binding == null) {
                                showResult()
                            } else {
                                val lottieAnimationView = LottieAnimationView(context)
                                lottieAnimationView.apply {
                                    layoutParams =
                                        RelativeLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        ).apply {
                                            addRule(RelativeLayout.CENTER_IN_PARENT)
                                        }
                                    setAnimation(R.raw.success)
                                    playAnimation()
                                    addAnimatorUpdateListener { animation ->
                                        if (animation.animatedFraction == 1.0F) {
                                            showResult()
                                        }
                                    }
                                }
                                binding?.relativeLayout?.addView(lottieAnimationView)
                            }
                            isProcessing = false
                        }
                    }
                }
                show()
            }
        }
    }
}