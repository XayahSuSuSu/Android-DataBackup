package com.xayah.databackup.fragment.backup

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.airbnb.lottie.LottieAnimationView
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.MainActivity
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.databinding.LayoutProcessingBinding
import com.xayah.databackup.util.*
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged
import com.xayah.design.view.setWithConfirm
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

    var currentAppName = MutableLiveData<String?>()
    var currentAppIcon = MutableLiveData<Drawable?>()

    var selectAllApp = false
    var selectAllData = false
    var selectAll = false

    var notification = Notification("backup", "Backup")

    fun initialize(context: Context, room: Room?, onInitialized: () -> Unit) {
        isFiltering = false

        val mContext = context as FragmentActivity

        // 设置底部栏菜单按钮
        binding?.bottomAppBar?.menu?.let { setSearchView(mContext, it) }
        binding?.bottomAppBar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.backup_reverse -> {
                    setReverse(mContext, room)
                }
            }
            true
        }

        // 确定事件
        binding?.floatingActionButton?.setOnClickListener {
            setConfirm(mContext) {
                binding?.coordinatorLayout?.removeView(binding?.bottomAppBar)
                binding?.coordinatorLayout?.removeView(binding?.floatingActionButton)
            }
        }

        // 加载进度
        val linearProgressIndicator = LinearProgressIndicator(mContext).apply { fastInitialize() }
        binding?.relativeLayout?.addView(linearProgressIndicator)
        if (!isProcessing) {
            // 没有Processing
            mAdapter = MultiTypeAdapter().apply {
                register(AppListAdapterBackup(room, mContext))
                CoroutineScope(Dispatchers.IO).launch {
                    // 按照字母表排序
                    val mAppList = Command.getAppList(mContext, room).apply {
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
                        if (appList.isEmpty()) {
                            binding?.linearLayout?.visibility = View.VISIBLE
                        } else {
                            binding?.linearLayout?.visibility = View.GONE
                        }
                        linearProgressIndicator.visibility = View.GONE
                        binding?.recyclerView?.visibility = View.VISIBLE
                        onInitialized()
                    }
                }
            }
        } else {
            // 恢复状态
            // 设置Processing布局
            onProcessing(mContext)
            onInitialized()
        }

        binding?.recyclerView?.apply {
            adapter = mAdapter
            fastInitialize()
        }
    }

    private fun setSearchView(context: Context, menu: Menu) {
        val searchView = SearchView(context).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        appList =
                            appListAll.filter {
                                it.appName.lowercase().contains(newText.lowercase())
                            }
                                .toMutableList()
                        mAdapter.items = appList
                        binding?.recyclerView?.notifyDataSetChanged()
                    }
                    return false
                }
            })
            queryHint = GlobalString.pleaseTypeKeyWord
            isQueryRefinementEnabled = true
        }
        val item = menu.findItem(R.id.backup_search).apply {
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
        searchView.setOnQueryTextFocusChangeListener { _, queryTextFocused ->
            if (!queryTextFocused) {
                item.collapseActionView()
                searchView.setQuery("", false)
            }
        }
    }

    private fun setReverse(context: FragmentActivity, room: Room?) {
        PopupMenu(context, context.findViewById(R.id.bottomAppBar)).apply {
            menuInflater.inflate(R.menu.select, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.select_all -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp = selectAll
                            appList[index].backupData = selectAll
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.selectAllApp(selectAll)
                                room?.selectAllData(selectAll)
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                        selectAll = !selectAll
                    }
                    R.id.select_all_app -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupApp = selectAllApp
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.selectAllApp(selectAllApp)
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                        selectAllApp = !selectAllApp
                    }
                    R.id.select_all_data -> {
                        for ((index, _) in appList.withIndex()) {
                            appList[index].backupData = selectAllData
                        }
                        if (!isFiltering)
                            CoroutineScope(Dispatchers.IO).launch {
                                room?.selectAllData(selectAllData)
                            }
                        binding?.recyclerView?.notifyDataSetChanged()
                        selectAllData = !selectAllData
                    }
                }
                true
            }
            show()
        }
    }

    private fun setConfirm(context: FragmentActivity, onCallback: () -> Unit) {
        if (isFiltering) {
            // 检查是否处于搜索模式
            Toast.makeText(
                context,
                GlobalString.pleaseExitSearchMode,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // 确认清单
            var contents = "${GlobalString.selected}\n"
            if (App.globalContext.readIsBackupItself()) {
                // 备份自身
                contents += "${GlobalString.appName}\n"
            }
            for (i in appListAll) {
                if (i.backupApp || i.backupData) {
                    if (i.backupApp)
                        contents += "[${GlobalString.application}]"
                    if (i.backupData)
                        contents += "[${GlobalString.data}]"
                    contents += " ${i.appName}\n"
                }
            }
            if (App.globalContext.readIsCustomDirectoryPath()) {
                // 自定义备份目录
                contents += App.globalContext.readCustomDirectoryPath().split("\n")
                    .joinToString(separator = "\n")
            }

            MaterialAlertDialogBuilder(context).apply {
                setWithConfirm(contents) {
                    // 初始化通知类
                    notification.initialize(context)
                    // 设置Processing布局
                    val layoutProcessingBinding = onProcessing(context)
                    // 清空日志
                    App.log.clear()
                    // 初始化数据
                    time = 0
                    index = 0
                    success = 0
                    failed = 0
                    isProcessing = true
                    // 标题栏计数
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
                                    "${GlobalString.backupProcessing}: ${index}/${total}"
                                // 更新通知
                                notification.update(index == total) {
                                    it?.setProgress(total, index, false)
                                    it?.setContentText("${index}/${total}")
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            (context as MainActivity).binding.toolbar.subtitle =
                                context.viewModel.versionName
                            context.binding.toolbar.title = GlobalString.backupSuccess
                        }
                    }
                    onCallback()

                    // 重建appList
                    appList = mutableListOf()
                    appList.addAll(appListAll)
                    mAdapter.items = appList
                    for (i in appListAll) {
                        if (!i.backupApp && !i.backupData) {
                            appList.remove(i)
                        } else {
                            appList[appList.indexOf(i)].isProcessing = true
                        }
                    }

                    // 设置用户
                    val userId = context.readBackupUser()

                    // 获取任务总个数
                    total = appList.size
                    if (App.globalContext.readIsCustomDirectoryPath()) {
                        // 自定义备份目录
                        total += App.globalContext.readCustomDirectoryPath().split("\n").size
                    }
                    if (App.globalContext.readIsBackupItself()) {
                        // 备份自身
                        total += 1
                        val appName = GlobalString.appName
                        val icon = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
                        val packageName = "com.xayah.databackup"
                        val outPut = context.readBackupSavePath()

                        // 更新通知
                        notification.update(false) {
                            it?.setContentTitle(appName)
                        }
                        // 推送数据
                        currentAppName.postValue(appName)
                        currentAppIcon.postValue(icon)
                        App.log.add("----------------------------")
                        App.log.add("${GlobalString.backupProcessing}: $packageName")
                        var state = true // 该任务是否成功完成
                        App.log.add(GlobalString.backupApkProcessing)
                        Command.backupItself(packageName, outPut, "")
                            .apply {
                                if (!this)
                                    state = false
                            }
                        App.log.add(GlobalString.success)
                        if (state)
                            success += 1
                        else
                            failed += 1
                        index++
                    }

                    // 建立备份目录
                    val outPut = "${context.readBackupSavePath()}/$userId"
                    Command.mkdir(outPut)

                    // 生成备份信息
                    val backupInfo = BackupInfo(GlobalString.backupVersion)
                    Command.object2JSONFile(
                        backupInfo,
                        "${outPut}/info"
                    ).apply {
                        if (!this) {
                            App.log.add(GlobalString.generateBackupInfoFailed)
                            isProcessing = false
                            showFinish(context)
                            return@setWithConfirm
                        }
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        for (i in appList) {
                            // 更新通知
                            notification.update(false) {
                                it?.setContentTitle(i.appName)
                            }
                            // 推送数据
                            currentAppName.postValue(i.appName)
                            currentAppIcon.postValue(i.icon)
                            App.log.add("----------------------------")
                            App.log.add("${GlobalString.backupProcessing}: ${i.packageName}")
                            var state = true // 该任务是否成功完成

                            // 设置任务参数
                            val compressionType = context.readCompressionType()
                            val packageName = i.packageName
                            val outPutData = "${outPut}/${packageName}"

                            if (i.backupApp) {
                                // 选中备份应用
                                App.log.add(GlobalString.backupApkProcessing)
                                Command.compressAPK(
                                    compressionType,
                                    packageName,
                                    outPutData,
                                    userId,
                                    i.appInfo?.apkSize
                                )
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                App.log.add(GlobalString.success)
                            }
                            if (i.backupData) {
                                // 选中备份数据
                                App.log.add("${GlobalString.backupProcessing}user")
                                // 备份user数据
                                Command.compress(
                                    compressionType,
                                    "user",
                                    packageName,
                                    outPutData,
                                    Path.getUserPath(userId),
                                    i.appInfo?.userSize
                                )
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                App.log.add("${GlobalString.backupProcessing}data")
                                // 备份data数据
                                Command.compress(
                                    compressionType,
                                    "data",
                                    packageName,
                                    outPutData,
                                    Path.getDataPath(userId),
                                    i.appInfo?.dataSize
                                )
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                                App.log.add("${GlobalString.backupProcessing}obb")
                                // 备份obb数据
                                Command.compress(
                                    compressionType,
                                    "obb",
                                    packageName,
                                    outPutData,
                                    Path.getObbPath(userId),
                                    i.appInfo?.obbSize
                                )
                                    .apply {
                                        if (!this)
                                            state = false
                                    }
                            }
                            // 生成应用信息
                            Command.generateAppInfo(
                                i.appName,
                                userId,
                                i.packageName,
                                Command.countSize(
                                    Bashrc.getAPKPath(i.packageName, userId).second,
                                    1
                                ),
                                Command.countSize(
                                    "${Path.getUserPath(userId)}/${i.packageName}",
                                    1
                                ),
                                Command.countSize(
                                    "${Path.getDataPath(userId)}/${i.packageName}",
                                    1
                                ),
                                Command.countSize(
                                    "${Path.getObbPath(userId)}/${i.packageName}",
                                    1
                                ),
                                outPutData,
                            )
                                .apply {
                                    if (!this)
                                        state = false
                                }
                            // 检验
                            Command.testArchiveForEach(outPutData).apply {
                                if (!this)
                                    state = false
                            }
                            if (state)
                                success += 1
                            else
                                failed += 1

                            index++
                        }
                        if (App.globalContext.readIsCustomDirectoryPath()) {
                            // 移除图标
                            withContext(Dispatchers.Main) {
                                layoutProcessingBinding.linearLayout.removeView(
                                    layoutProcessingBinding.shapeableImageViewAppIcon
                                )
                            }

                            // 备份自定义目录
                            val outPutMedia = "${outPut}/media"

                            // 目录数组
                            val jsonArray = JsonArray()

                            // 已存在的数据
                            val info = Shell.cmd("cat ${outPut}/media/info")
                                .exec().out.joinToString()
                            val jsonArrayInfo = JsonArray()

                            try {
                                JsonParser.parseString(info).asJsonArray
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            for (i in App.globalContext.readCustomDirectoryPath().split("\n")) {
                                // 更新通知
                                notification.update(false) {
                                    it?.setContentTitle(i)
                                }
                                // 推送数据
                                currentAppName.postValue(i)
                                App.log.add("----------------------------")
                                var state = true // 该任务是否成功完成

                                // 目录信息
                                val name = i.split("/").last()

                                val mediaInfo = MediaInfo(
                                    name,
                                    i.replace("/${name}", ""),
                                    Command.countSize(i, 1)
                                )

                                try {
                                    jsonArray.add(JsonParser.parseString(Gson().toJson(mediaInfo)))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                var size: String? = null

                                for (j in jsonArrayInfo) {
                                    try {
                                        val item = Gson().fromJson(j, MediaInfo::class.java)
                                        if (item.name == name.split(".").first())
                                            size = item.size
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }

                                // 备份目录
                                Command.compress(
                                    App.globalContext.readCompressionType(),
                                    "media",
                                    "media",
                                    outPutMedia,
                                    i,
                                    size
                                ).apply {
                                    if (!this)
                                        state = false
                                }
                                // 检验
                                Command.testArchiveForEach(outPutMedia).apply {
                                    if (!this)
                                        state = false
                                }
                                if (state)
                                    success += 1
                                else
                                    failed += 1
                                index++
                            }

                            // 生成目录信息
                            Command.object2JSONFile(
                                jsonArray,
                                "${outPutMedia}/info"
                            ).apply {
                                if (!this) {
                                    App.log.add(GlobalString.generateMediaInfoFailed)
                                    isProcessing = false
                                    showFinish(context)
                                    return@launch
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            showFinish(context)
                        }
                    }
                }
            }
        }
    }

    private fun onProcessing(context: FragmentActivity): LayoutProcessingBinding {
        // 移除底部栏和悬浮按钮
        binding?.coordinatorLayout?.removeView(binding?.bottomAppBar)
        binding?.coordinatorLayout?.removeView(binding?.floatingActionButton)
        // 载入Processing布局
        val layoutProcessingBinding =
            LayoutProcessingBinding.inflate(LayoutInflater.from(context)).apply {
                root.apply {
                    layoutParams =
                        RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                            .apply {
                                addRule(RelativeLayout.CENTER_IN_PARENT)
                            }
                }
            }
        binding?.relativeLayout?.removeAllViews()
        binding?.relativeLayout?.addView(layoutProcessingBinding.root)
        // 绑定可变数据
        currentAppName.observe(context) {
            it?.apply {
                layoutProcessingBinding.materialTextViewAppName.text = this
            }
        }
        currentAppIcon.observe(context) {
            it?.apply {
                layoutProcessingBinding.shapeableImageViewAppIcon.setImageDrawable(this)
            }
        }
        App.log.onObserveLast(context) {
            it?.apply {
                layoutProcessingBinding.materialTextViewLog.text = this
            }
        }
        return layoutProcessingBinding
    }

    private fun showFinish(context: Context) {
        // 完成通知
        notification.update(true) {
            it?.setContentTitle(GlobalString.backupSuccess)
        }
        // 展示完成页面
        binding?.relativeLayout?.removeAllViews()
        val showResult = {
            Toast.makeText(
                context,
                GlobalString.backupSuccess,
                Toast.LENGTH_SHORT
            ).show()
            BottomSheetDialog(context).apply {
                val s = String.format("%02d", time % 60)
                val m = String.format("%02d", time / 60 % 60)
                val h = String.format("%02d", time / 3600 % 24)
                setWithResult(
                    App.log.toString(),
                    success,
                    failed,
                    "$h:$m:$s",
                    Command.countSize(context.readBackupSavePath())
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

    fun clear() {
        binding = null

        isProcessing = false

        appList = mutableListOf()

        appListAll = mutableListOf()

        isFiltering = false

        mAdapter = MultiTypeAdapter()

        time = 0
        index = 0
        total = 0

        success = 0
        failed = 0

        currentAppName = MutableLiveData<String?>()
        currentAppIcon = MutableLiveData<Drawable?>()

        selectAllApp = false
        selectAllData = false
        selectAll = false

        notification = Notification("backup", "Backup")
    }
}