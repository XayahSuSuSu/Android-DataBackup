package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.AnimationUtils.loadAnimation
import android.view.animation.LayoutAnimationController
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
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
import com.xayah.databackup.util.dp
import com.xayah.databackup.util.readPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BackupFragment : Fragment() {
    private var _binding: FragmentBackupBinding? = null

    private val binding get() = _binding!!

    lateinit var room: Room

    lateinit var mAdapter: MultiTypeAdapter

    lateinit var appList: MutableList<AppEntity>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = ViewModelProvider(this)[BackupViewModel::class.java]

        initialize()
    }

    private fun initialize() {
        val mContext = requireContext()
        room = Room(mContext)
        setHasOptionsMenu(true)

        val linearProgressIndicator = LinearProgressIndicator(mContext).apply {
            layoutParams =
                RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    .apply {
                        addRule(RelativeLayout.CENTER_IN_PARENT)
                        marginStart = 100.dp
                        marginEnd = 100.dp
                    }
            trackCornerRadius = 3.dp
            isIndeterminate = true
        }
        binding.relativeLayout.addView(linearProgressIndicator)
        mAdapter = MultiTypeAdapter().apply {
            register(AppListAdapter(room))
            CoroutineScope(Dispatchers.IO).launch {
                appList = Command.getAppList(mContext, room)
                items = appList
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                    linearProgressIndicator.visibility = View.GONE
                    if (_binding != null)
                        binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
        binding.recyclerView.apply {
            adapter = mAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            layoutManager = GridLayoutManager(mContext, 1)
            visibility = View.INVISIBLE
            layoutAnimation = LayoutAnimationController(
                loadAnimation(
                    context,
                    androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom
                )
            ).apply {
                order = LayoutAnimationController.ORDER_NORMAL
                delay = 0.3F
            }
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
                val items: Array<String> = resources.getStringArray(R.array.reverse_array)
                var choice = 0
                MaterialAlertDialogBuilder(mContext)
                    .setTitle(getString(R.string.choose))
                    .setCancelable(true)
                    .setSingleChoiceItems(
                        items,
                        choice
                    ) { _, which ->
                        choice = which
                    }
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        when (choice) {
                            0 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupApp = true
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room.selectAllApp()
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            1 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupData = true
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room.selectAllData()
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            2 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupApp = !appList[index].backupApp
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room.reverseAllApp()
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            3 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupData = !appList[index].backupData
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    room.reverseAllData()
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    .show()
            }
            R.id.backup_confirm -> {
                binding.recyclerView.scrollToPosition(0)
                val mAppList = mutableListOf<AppEntity>()
                mAppList.addAll(appList)
                for (i in mAppList) {
                    if (!i.backupApp && !i.backupData) {
                        appList.remove(i)
                    } else {
                        appList[appList.indexOf(i)].isProcessing = true
                    }
                }
                mAdapter.notifyDataSetChanged()
                mAppList.clear()
                mAppList.addAll(appList)
                CoroutineScope(Dispatchers.IO).launch {
                    for (i in mAppList) {
                        val compressionType = mContext.readPreferences("compression_type") ?: "lz4"
                        val packageName = i.packageName
                        val outPut =
                            "${mContext.readPreferences("backup_save_path") ?: mContext.getString(R.string.default_backup_save_path)}/${packageName}"

                        if (appList[0].backupApp) {
                            withContext(Dispatchers.Main) {
                                appList[0].onProcessingApp = true
                                mAdapter.notifyItemChanged(0)
                                appList[0].progress = getString(R.string.backup_apk_processing)
                                mAdapter.notifyItemChanged(0)
                            }
                            Command.compressAPK(compressionType, packageName, outPut)
                            withContext(Dispatchers.Main) {
                                appList[0].onProcessingApp = false
                                appList[0].backupApp = false
                                mAdapter.notifyItemChanged(0)
                            }
                        }
                        if (appList[0].backupData) {
                            withContext(Dispatchers.Main) {
                                appList[0].onProcessingData = true
                                mAdapter.notifyItemChanged(0)
                            }
                            Command.compress(compressionType, "user", packageName, outPut) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (appList.isNotEmpty()) {
                                        appList[0].progress = it
                                        mAdapter.notifyItemChanged(0)
                                    }
                                }
                            }
                            Command.compress(compressionType, "data", packageName, outPut) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (appList.isNotEmpty()) {
                                        appList[0].progress = it
                                        mAdapter.notifyItemChanged(0)
                                    }
                                }
                            }
                            Command.compress(compressionType, "obb", packageName, outPut) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (appList.isNotEmpty()) {
                                        appList[0].progress = it
                                        mAdapter.notifyItemChanged(0)
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            appList.removeAt(0)
                            mAdapter.notifyItemRemoved(0)
                        }
                        Command.generateAppInfo(i.appName, i.packageName, outPut)
                    }
                    withContext(Dispatchers.Main) {
                        val lottieAnimationView = LottieAnimationView(context)
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
                                    Toast.makeText(
                                        mContext,
                                        context.getString(R.string.backup_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        binding.relativeLayout.addView(lottieAnimationView)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        room.close()
    }
}