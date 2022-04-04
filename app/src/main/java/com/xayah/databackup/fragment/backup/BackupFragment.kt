package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.xayah.databackup.util.readPreferences
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged
import com.xayah.design.view.setWithResult
import kotlinx.coroutines.*

class BackupFragment : Fragment() {
    lateinit var viewModel: BackupViewModel

    private var room: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[BackupViewModel::class.java]
        viewModel.binding?.viewModel = viewModel
        viewModel.binding = FragmentBackupBinding.inflate(inflater, container, false)
        return viewModel.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()
    }

    private fun initialize() {
        val mContext = requireActivity()
        room = Room(mContext)

        val linearProgressIndicator = LinearProgressIndicator(mContext).apply { fastInitialize() }
        viewModel.binding?.relativeLayout?.addView(linearProgressIndicator)
        if (!viewModel.isProcessing) {
            viewModel.mAdapter = MultiTypeAdapter().apply {
                register(AppListAdapter(room))
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.appList = Command.getAppList(mContext, room)
                    items = viewModel.appList
                    withContext(Dispatchers.Main) {
                        viewModel.binding?.recyclerView?.notifyDataSetChanged()
                        linearProgressIndicator.visibility = View.GONE
                        viewModel.binding?.recyclerView?.visibility = View.VISIBLE
                        setHasOptionsMenu(true)
                    }
                }
            }
        } else {
            linearProgressIndicator.visibility = View.GONE
            viewModel.binding?.recyclerView?.visibility = View.VISIBLE
            setHasOptionsMenu(true)
        }
        viewModel.binding?.recyclerView?.apply {
            adapter = viewModel.mAdapter
            fastInitialize()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.backup, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val mContext = requireActivity()
        when (item.itemId) {
            R.id.backup_reverse -> {
                val items: Array<String> = mContext.resources.getStringArray(R.array.reverse_array)
                var choice = 0
                MaterialAlertDialogBuilder(mContext).apply {
                    setTitle(mContext.getString(R.string.choose))
                    setCancelable(true)
                    setSingleChoiceItems(
                        items,
                        choice
                    ) { _, which ->
                        choice = which
                    }
                    setPositiveButton(mContext.getString(R.string.confirm)) { _, _ ->
                        when (choice) {
                            0 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupApp = true
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.selectAllApp()
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            1 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData = true
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.selectAllData()
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            2 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupApp =
                                        !viewModel.appList[index].backupApp
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.reverseAllApp()
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            3 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData =
                                        !viewModel.appList[index].backupData
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.reverseAllData()
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                        }
                    }
                    show()
                }
            }
            R.id.backup_confirm -> {
                MaterialAlertDialogBuilder(mContext).apply {
                    setTitle(mContext.getString(R.string.tips))
                    setCancelable(true)
                    setMessage(mContext.getString(R.string.onConfirm))
                    setNegativeButton(mContext.getString(R.string.cancel)) { _, _ -> }
                    setPositiveButton(mContext.getString(R.string.confirm)) { _, _ ->
                        App.log.clear()
                        viewModel.time = 0
                        viewModel.success = 0
                        viewModel.failed = 0
                        viewModel.isProcessing = true
                        CoroutineScope(Dispatchers.IO).launch {
                            while (viewModel.isProcessing) {
                                delay(1000)
                                viewModel.time += 1
                                val s = String.format("%02d", viewModel.time % 60)
                                val m = String.format("%02d", viewModel.time / 60 % 60)
                                val h = String.format("%02d", viewModel.time / 3600 % 24)
                                withContext(Dispatchers.Main) {
                                    (mContext as MainActivity).binding.toolbar.subtitle = "$h:$m:$s"
                                    mContext.binding.toolbar.title =
                                        "${mContext.getString(R.string.backup_processing)}: ${viewModel.index}/${viewModel.total}"
                                }
                            }
                            withContext(Dispatchers.Main) {
                                (mContext as MainActivity).binding.toolbar.subtitle =
                                    mContext.viewModel.versionName
                                mContext.binding.toolbar.title =
                                    mContext.getString(R.string.backup_success)
                            }
                        }
                        viewModel.binding?.recyclerView?.scrollToPosition(0)
                        setHasOptionsMenu(false)

                        val mAppList = mutableListOf<AppEntity>()
                        mAppList.addAll(viewModel.appList)
                        for (i in mAppList) {
                            if (!i.backupApp && !i.backupData) {
                                viewModel.appList.remove(i)
                            } else {
                                viewModel.appList[viewModel.appList.indexOf(i)].isProcessing = true
                            }
                        }
                        viewModel.binding?.recyclerView?.notifyDataSetChanged()
                        mAppList.clear()
                        mAppList.addAll(viewModel.appList)
                        viewModel.total = mAppList.size
                        CoroutineScope(Dispatchers.IO).launch {
                            for ((index, i) in mAppList.withIndex()) {
                                App.log.add("----------------------------")
                                App.log.add("${mContext.getString(R.string.backup_processing)}: ${i.packageName}")
                                var state = true
                                viewModel.index = index
                                val compressionType =
                                    mContext.readPreferences("compression_type") ?: "zstd"
                                val packageName = i.packageName
                                val outPut =
                                    "${
                                        mContext.readPreferences("backup_save_path") ?: mContext.getString(
                                            R.string.default_backup_save_path
                                        )
                                    }/${packageName}"

                                if (viewModel.appList[0].backupApp) {
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingApp = true
                                        viewModel.mAdapter.notifyItemChanged(0)
                                        viewModel.appList[0].progress =
                                            mContext.getString(R.string.backup_apk_processing)
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.compressAPK(compressionType, packageName, outPut)
                                        .apply {
                                            if (!this)
                                                state = false
                                        }
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingApp = false
                                        viewModel.appList[0].backupApp = false
                                        viewModel.appList[0].progress =
                                            mContext.getString(R.string.success)
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                }
                                if (viewModel.appList[0].backupData) {
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingData = true
                                        viewModel.appList[0].progress =
                                            "${mContext.getString(R.string.backup_processing)}user"
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.compress(compressionType, "user", packageName, outPut)
                                        .apply {
                                            if (!this)
                                                state = false
                                        }
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].progress =
                                            "${mContext.getString(R.string.backup_processing)}data"
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.compress(compressionType, "data", packageName, outPut)
                                        .apply {
                                            if (!this)
                                                state = false
                                        }
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].progress =
                                            "${mContext.getString(R.string.backup_processing)}obb"
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.compress(compressionType, "obb", packageName, outPut)
                                        .apply {
                                            if (!this)
                                                state = false
                                        }
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.appList.removeAt(0)
                                    viewModel.mAdapter.notifyItemRemoved(0)
                                }
                                Command.generateAppInfo(i.appName, i.packageName, outPut).apply {
                                    if (!this)
                                        state = false
                                }
                                if (state)
                                    viewModel.success += 1
                                else
                                    viewModel.failed += 1
                            }
                            withContext(Dispatchers.Main) {
                                val showResult = {
                                    Toast.makeText(
                                        mContext,
                                        mContext.getString(R.string.backup_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    BottomSheetDialog(mContext).apply {
                                        setWithResult(
                                            App.log.toString(),
                                            viewModel.success,
                                            viewModel.failed
                                        )
                                    }
                                }
                                if (viewModel.binding == null) {
                                    showResult()
                                } else {
                                    val lottieAnimationView = LottieAnimationView(mContext)
                                    lottieAnimationView.apply {
                                        layoutParams =
                                            RelativeLayout.LayoutParams(
                                                LayoutParams.MATCH_PARENT,
                                                LayoutParams.MATCH_PARENT
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
                                    viewModel.binding?.relativeLayout?.addView(lottieAnimationView)
                                }
                                viewModel.isProcessing = false
                            }
                        }
                    }
                    show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.binding = null
        room?.close()
        room = null
    }
}