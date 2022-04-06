package com.xayah.databackup.fragment.restore

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.xayah.databackup.util.readPreferences
import com.xayah.design.util.dp
import com.xayah.design.view.fastInitialize
import com.xayah.design.view.notifyDataSetChanged
import com.xayah.design.view.setWithResult
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.*

class RestoreFragment : Fragment() {
    lateinit var viewModel: RestoreViewModel

    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity())[RestoreViewModel::class.java]
        viewModel.binding?.viewModel = viewModel

        viewModel.binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return viewModel.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val mContext = requireActivity()

        if (!viewModel.isProcessing) {
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
            viewModel.binding?.relativeLayout?.addView(lottieAnimationView)
            val materialButton = MaterialButton(mContext).apply {
                id = MaterialButton.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, lottieAnimationView.id)
                }
                text = mContext.getString(R.string.choose_backup)
                setOnClickListener {
                    materialYouFileExplorer.toExplorer(
                        mContext, false, "default", arrayListOf(), true
                    ) { path, _ ->
                        viewModel.appList = mutableListOf()
                        viewModel.appListAll = mutableListOf()
                        val packages = Shell.cmd("ls $path").exec().out
                        for (i in packages) {
                            val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                            try {
                                val appName = info[0].split("=")
                                val packageName = info[1].split("=")
                                val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                                    icon = AppCompatResources.getDrawable(
                                        mContext, R.drawable.ic_round_android
                                    )
                                    backupPath = "${path}/${i}"
                                }
                                viewModel.appList.add(appEntity)
                                viewModel.appListAll.add(appEntity)
                            } catch (e: IndexOutOfBoundsException) {
                                e.printStackTrace()
                            }
                        }
                        if (viewModel.appList.isEmpty()) {
                            Toast.makeText(
                                mContext,
                                mContext.getString(R.string.choose_right_backup),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            setHasOptionsMenu(true)
                            viewModel.binding?.relativeLayout?.removeView(this)
                            viewModel.binding?.relativeLayout?.removeView(lottieAnimationView)
                            showAppList(mContext)
                        }
                    }
                }
            }
            val materialButtonDef = MaterialButton(mContext).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16.dp
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.BELOW, materialButton.id)
                }
                text = mContext.getString(R.string.choose_backup_def)
                setOnClickListener {
                    viewModel.appList = mutableListOf()
                    viewModel.appListAll = mutableListOf()
                    val path = mContext.readPreferences("backup_save_path")
                        ?: mContext.getString(R.string.default_backup_save_path)
                    val packages = Shell.cmd("ls $path").exec().out
                    for (i in packages) {
                        val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                        try {
                            val appName = info[0].split("=")
                            val packageName = info[1].split("=")
                            val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                                icon = AppCompatResources.getDrawable(
                                    mContext, R.drawable.ic_round_android
                                )
                                backupPath = "${path}/${i}"
                            }
                            viewModel.appList.add(appEntity)
                            viewModel.appListAll.add(appEntity)
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace()
                        }
                    }
                    if (viewModel.appList.isEmpty()) {
                        Toast.makeText(
                            mContext,
                            mContext.getString(R.string.choose_right_backup),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setHasOptionsMenu(true)
                        viewModel.binding?.relativeLayout?.removeView(this)
                        viewModel.binding?.relativeLayout?.removeView(materialButton)
                        viewModel.binding?.relativeLayout?.removeView(lottieAnimationView)
                        showAppList(mContext)
                    }
                }
            }

            viewModel.binding?.relativeLayout?.addView(materialButton)
            viewModel.binding?.relativeLayout?.addView(materialButtonDef)
        } else {
            showAppList(mContext)
        }
    }

    private fun showAppList(mContext: Context) {
        val linearProgressIndicator = LinearProgressIndicator(mContext).apply { fastInitialize() }
        viewModel.binding?.relativeLayout?.addView(linearProgressIndicator)
        viewModel.mAdapter = MultiTypeAdapter().apply {
            register(AppListAdapter(null))
            CoroutineScope(Dispatchers.IO).launch {
                items = viewModel.appList
                withContext(Dispatchers.Main) {
                    viewModel.binding?.recyclerView?.notifyDataSetChanged()
                    linearProgressIndicator.visibility = View.GONE
                    viewModel.binding?.recyclerView?.visibility = View.VISIBLE
                }
            }
        }
        viewModel.binding?.recyclerView?.apply {
            adapter = viewModel.mAdapter
            fastInitialize()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.backup, menu)

        val searchView = SearchView(requireContext()).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        viewModel.appList =
                            viewModel.appListAll.filter { it.appName.contains(newText) }
                                .toMutableList()
                        viewModel.mAdapter.items = viewModel.appList
                        viewModel.binding?.recyclerView?.notifyDataSetChanged()
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
                    viewModel.isFiltering = true
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    viewModel.isFiltering = false
                    return true
                }
            })
        }
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
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            1 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData = true
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            2 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupApp =
                                        !viewModel.appList[index].backupApp
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                            3 -> {
                                for ((index, _) in viewModel.appList.withIndex()) {
                                    viewModel.appList[index].backupData =
                                        !viewModel.appList[index].backupData
                                }
                                viewModel.binding?.recyclerView?.notifyDataSetChanged()
                            }
                        }
                    }
                    show()
                }
            }
            R.id.backup_confirm -> {
                if (viewModel.isFiltering) {
                    Toast.makeText(
                        mContext,
                        mContext.getString(R.string.please_exit_search_mode),
                        Toast.LENGTH_SHORT
                    ).show()
                    return super.onOptionsItemSelected(item)
                }
                MaterialAlertDialogBuilder(mContext).apply {
                    setTitle(mContext.getString(R.string.tips))
                    setCancelable(true)
                    setMessage(mContext.getString(R.string.onConfirm))
                    setNegativeButton(mContext.getString(R.string.cancel)) { _, _ -> }
                    setPositiveButton(mContext.getString(R.string.confirm)) { _, _ ->
                        setHasOptionsMenu(false)
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
                                        "${mContext.getString(R.string.restore_processing)}: ${viewModel.index}/${viewModel.total}"
                                }
                            }
                            withContext(Dispatchers.Main) {
                                (mContext as MainActivity).binding.toolbar.subtitle =
                                    mContext.viewModel.versionName
                                mContext.binding.toolbar.title =
                                    mContext.getString(R.string.restore_success)
                            }
                        }
                        viewModel.binding?.recyclerView?.scrollToPosition(0)

                        viewModel.appList.clear()
                        viewModel.appList.addAll(viewModel.appListAll)
                        viewModel.mAdapter.items = viewModel.appList

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
                                App.log.add("${mContext.getString(R.string.restore_processing)}: ${i.packageName}")
                                var state = true
                                viewModel.index = index
                                val inPath = i.backupPath
                                val packageName = i.packageName

                                if (viewModel.appList[0].backupApp) {
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingApp = true
                                        viewModel.mAdapter.notifyItemChanged(0)
                                        viewModel.appList[0].progress =
                                            mContext.getString(R.string.install_apk_processing)
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.installAPK(inPath, packageName)
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingApp = false
                                        viewModel.appList[0].backupApp = false
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                }
                                if (viewModel.appList[0].backupData) {
                                    withContext(Dispatchers.Main) {
                                        viewModel.appList[0].onProcessingData = true
                                        viewModel.appList[0].progress =
                                            mContext.getString(R.string.restore_processing)
                                        viewModel.mAdapter.notifyItemChanged(0)
                                    }
                                    Command.restoreData(packageName, inPath).apply {
                                        if (!this)
                                            state = false
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.appList.removeAt(0)
                                    viewModel.mAdapter.notifyItemRemoved(0)
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
                                        mContext.getString(R.string.restore_success),
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
    }
}