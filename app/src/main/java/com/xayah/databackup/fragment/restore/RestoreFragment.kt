package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListAdapter
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.dp
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RestoreFragment : Fragment() {
    private var _binding: FragmentRestoreBinding? = null

    private val binding get() = _binding!!

    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    lateinit var mAdapter: MultiTypeAdapter

    val appList: MutableList<AppEntity> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = ViewModelProvider(this)[RestoreViewModel::class.java]

        initialize()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val that = this
        materialYouFileExplorer = MaterialYouFileExplorer().apply {
            initialize(that)
        }
    }

    private fun initialize() {
        val mContext = requireContext()

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
        binding.relativeLayout.addView(lottieAnimationView)
        val materialButton = MaterialButton(mContext).apply {
            layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, lottieAnimationView.id)
                }
            text = mContext.getString(R.string.choose_backup)
            setOnClickListener {
                materialYouFileExplorer.toExplorer(
                    mContext,
                    false,
                    "default",
                    arrayListOf(),
                    true
                ) { path, _ ->
                    val packages = Shell.cmd("ls $path").exec().out
                    for (i in packages) {
                        val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                        try {
                            val appName = info[0].split("=")
                            val packageName = info[1].split("=")
                            val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                                icon = AppCompatResources.getDrawable(
                                    mContext,
                                    R.drawable.ic_round_android
                                )
                                backupPath = "${path}/${i}"
                            }
                            appList.add(appEntity)
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace()
                        }
                    }
                    if (appList.isEmpty()) {
                        Toast.makeText(
                            mContext,
                            mContext.getString(R.string.choose_right_backup),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setHasOptionsMenu(true)
                        binding.relativeLayout.removeView(this)
                        binding.relativeLayout.removeView(lottieAnimationView)
                        val linearProgressIndicator = LinearProgressIndicator(mContext).apply {
                            layoutParams =
                                RelativeLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
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
                            register(AppListAdapter(null))
                            CoroutineScope(Dispatchers.IO).launch {
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
                                AnimationUtils.loadAnimation(
                                    context,
                                    androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom
                                )
                            ).apply {
                                order = LayoutAnimationController.ORDER_NORMAL
                                delay = 0.3F
                            }
                        }
                    }
                }
            }
        }
        binding.relativeLayout.addView(materialButton)
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
                                mAdapter.notifyDataSetChanged()
                            }
                            1 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupData = true
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            2 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupApp = !appList[index].backupApp
                                }
                                mAdapter.notifyDataSetChanged()
                            }
                            3 -> {
                                for ((index, _) in appList.withIndex()) {
                                    appList[index].backupData = !appList[index].backupData
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
                        val inPath = i.backupPath
                        val packageName = i.packageName

                        if (appList[0].backupApp) {
                            withContext(Dispatchers.Main) {
                                appList[0].onProcessingApp = true
                                mAdapter.notifyItemChanged(0)
                                appList[0].progress = getString(R.string.install_apk_processing)
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
                                mAdapter.notifyItemChanged(0)
                            }
                            Command.restoreData(packageName, inPath) {
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
                    }
                    withContext(Dispatchers.Main) {
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
                                    Toast.makeText(
                                        mContext,
                                        context.getString(R.string.restore_success),
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
    }
}