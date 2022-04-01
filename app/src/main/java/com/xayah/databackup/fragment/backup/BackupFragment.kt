package com.xayah.databackup.fragment.backup

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Room
import com.xayah.databackup.util.readPreferences
import com.xayah.design.view.fastInitialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NotifyDataSetChanged")
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
            setHasOptionsMenu(true)
            viewModel.mAdapter = MultiTypeAdapter().apply {
                register(AppListAdapter(room))
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.appList = Command.getAppList(mContext, room)
                    items = viewModel.appList
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                        linearProgressIndicator.visibility = View.GONE
                        viewModel.binding?.recyclerView?.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            linearProgressIndicator.visibility = View.GONE
            viewModel.binding?.recyclerView?.visibility = View.VISIBLE
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
        val mContext = requireContext()
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
                                viewModel.mAdapter.notifyDataSetChanged()
                            }
                            1 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData = true
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.selectAllData()
                                }
                                viewModel.mAdapter.notifyDataSetChanged()
                            }
                            2 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupApp =
                                        !viewModel.appList[index].backupApp
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.reverseAllApp()
                                }
                                viewModel.mAdapter.notifyDataSetChanged()
                            }
                            3 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData =
                                        !viewModel.appList[index].backupData
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room?.reverseAllData()
                                }
                                viewModel.mAdapter.notifyDataSetChanged()
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
                        viewModel.isProcessing = true
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
                        viewModel.mAdapter.notifyDataSetChanged()
                        mAppList.clear()
                        mAppList.addAll(viewModel.appList)
                        CoroutineScope(Dispatchers.IO).launch {
                            for (i in mAppList) {
                                val compressionType =
                                    mContext.readPreferences("compression_type") ?: "lz4"
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
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingApp = false
                                        viewModel.appList[0].backupApp = false
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                }
                                if (viewModel.appList[0].backupData) {
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingData = true
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.compress(compressionType, "user", packageName, outPut) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            if (viewModel.appList.isNotEmpty()) {
                                                viewModel.appList[0].progress = it
                                                viewModel.mAdapter.notifyItemChanged(0)
                                            }
                                        }
                                    }
                                    Command.compress(compressionType, "data", packageName, outPut) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            if (viewModel.appList.isNotEmpty()) {
                                                viewModel.appList[0].progress = it
                                                viewModel.mAdapter.notifyItemChanged(0)
                                            }
                                        }
                                    }
                                    Command.compress(compressionType, "obb", packageName, outPut) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            if (viewModel.appList.isNotEmpty()) {
                                                viewModel.appList[0].progress = it
                                                viewModel.mAdapter.notifyItemChanged(0)
                                            }
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.appList.removeAt(0)
                                    viewModel.mAdapter.notifyItemRemoved(0)
                                }
                                Command.generateAppInfo(i.appName, i.packageName, outPut)
                            }
                            withContext(Dispatchers.Main) {
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
                                        if (animation.animatedFraction == 1.0F || viewModel.binding == null) {
                                            Toast.makeText(
                                                mContext,
                                                mContext.getString(R.string.backup_success),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                viewModel.binding?.relativeLayout?.addView(lottieAnimationView)
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