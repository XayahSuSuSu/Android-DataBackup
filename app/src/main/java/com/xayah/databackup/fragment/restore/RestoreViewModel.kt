package com.xayah.databackup.fragment.restore

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.MainActivity
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.design.util.dp
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged
import com.xayah.design.view.setWithResult
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.*

class RestoreViewModel : ViewModel() {
    var binding: FragmentRestoreBinding? = null

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

    fun initialize(
        context: Context,
        materialYouFileExplorer: MaterialYouFileExplorer,
        onInitialized: () -> Unit
    ) {
        if (!isProcessing) {
            val lottieAnimationView = LottieAnimationView(context)
            lottieAnimationView.apply {
                id = LottieAnimationView.generateViewId()
                layoutParams =
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        200.dp
                    ).apply {
                        addRule(RelativeLayout.CENTER_IN_PARENT)
                    }
                setAnimation(R.raw.file)
                playAnimation()
                repeatCount = LottieDrawable.INFINITE
            }
            binding?.relativeLayout?.addView(lottieAnimationView)
            val materialButton = MaterialButton(context).apply {
                id = MaterialButton.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, lottieAnimationView.id)
                }
                text = context.getString(R.string.choose_backup)
                setOnClickListener {
                    materialYouFileExplorer.toExplorer(
                        context, false, "default", arrayListOf(), true
                    ) { path, _ ->
                        appList = mutableListOf()
                        appListAll = mutableListOf()
                        val packages = Shell.cmd("ls $path").exec().out
                        for (i in packages) {
                            val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                            try {
                                val appName = info[0].split("=")
                                val packageName = info[1].split("=")
                                val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                                    icon = AppCompatResources.getDrawable(
                                        context, R.drawable.ic_round_android
                                    )
                                    backupPath = "${path}/${i}"
                                }
                                appList.add(appEntity)
                                appListAll.add(appEntity)
                            } catch (e: IndexOutOfBoundsException) {
                                e.printStackTrace()
                            }
                        }
                        if (appList.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.choose_right_backup),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding?.relativeLayout?.removeView(this)
                            binding?.relativeLayout?.removeView(lottieAnimationView)
                            showAppList(context)
                            onInitialized()
                        }
                    }
                }
            }
            val materialButtonDef = MaterialButton(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16.dp
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, materialButton.id)
                }
                text = context.getString(R.string.choose_backup_def)
                setOnClickListener {
                    appList = mutableListOf()
                    appListAll = mutableListOf()
                    val path = context.readBackupSavePath()
                    val packages = Shell.cmd("ls $path").exec().out
                    for (i in packages) {
                        val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                        try {
                            val appName = info[0].split("=")
                            val packageName = info[1].split("=")
                            val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                                icon = AppCompatResources.getDrawable(
                                    context, R.drawable.ic_round_android
                                )
                                backupPath = "${path}/${i}"
                            }
                            appList.add(appEntity)
                            appListAll.add(appEntity)
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace()
                        }
                    }
                    if (appList.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.choose_right_backup),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onInitialized()
                        binding?.relativeLayout?.removeView(this)
                        binding?.relativeLayout?.removeView(materialButton)
                        binding?.relativeLayout?.removeView(lottieAnimationView)
                        showAppList(context)
                    }
                }
            }

            binding?.relativeLayout?.addView(materialButton)
            binding?.relativeLayout?.addView(materialButtonDef)
        } else {
            showAppList(context)
        }
    }

    private fun showAppList(context: Context) {
        val linearProgressIndicator = LinearProgressIndicator(context).apply { fastInitialize() }
        binding?.relativeLayout?.addView(linearProgressIndicator)
        mAdapter = MultiTypeAdapter().apply {
            register(AppListAdapter(null))
            CoroutineScope(Dispatchers.IO).launch {
                items = appList
                withContext(Dispatchers.Main) {
                    binding?.recyclerView?.notifyDataSetChanged()
                    linearProgressIndicator.visibility = View.GONE
                    binding?.recyclerView?.visibility = View.VISIBLE
                }
            }
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

    fun setReverse(context: FragmentActivity) {
        PopupMenu(context, context.findViewById(R.id.backup_reverse)).apply {
            menuInflater.inflate(R.menu.select, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.select_all_app -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp = true
                        }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.select_all_data -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupData = true
                        }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.reverse_all_app -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp =
                                !appList[index].backupApp
                        }
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    R.id.reverse_all_data -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupData =
                                !appList[index].backupData
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
                    onCallback()
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
                                    "${context.getString(R.string.restore_processing)}: ${index}/${total}"
                            }
                        }
                        withContext(Dispatchers.Main) {
                            (context as MainActivity).binding.toolbar.subtitle =
                                context.viewModel.versionName
                            context.binding.toolbar.title =
                                context.getString(R.string.restore_success)
                        }
                    }
                    binding?.recyclerView?.scrollToPosition(0)

                    appList.clear()
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
                            App.log.add("${context.getString(R.string.restore_processing)}: ${i.packageName}")
                            var state = true
                            index = mIndex
                            val inPath = i.backupPath
                            val packageName = i.packageName

                            if (appList[0].backupApp) {
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingApp = true
                                    mAdapter.notifyItemChanged(0)
                                    appList[0].progress =
                                        context.getString(R.string.install_apk_processing)
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.installAPK(inPath, packageName)
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingApp = false
                                    appList[0].backupApp = false
                                    mAdapter.notifyItemChanged(0)
                                }
                            }
                            if (appList[0].backupData) {
                                withContext(Dispatchers.Main) {
                                    appList[0].onProcessingData = true
                                    appList[0].progress =
                                        context.getString(R.string.restore_processing)
                                    mAdapter.notifyItemChanged(0)
                                }
                                Command.restoreData(packageName, inPath).apply {
                                    if (!this)
                                        state = false
                                }
                            }
                            withContext(Dispatchers.Main) {
                                appList.removeAt(0)
                                mAdapter.notifyItemRemoved(0)
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
                                    context.getString(R.string.restore_success),
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