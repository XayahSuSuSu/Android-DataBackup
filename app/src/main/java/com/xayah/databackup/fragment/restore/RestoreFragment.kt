package com.xayah.databackup.fragment.restore

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.FileActivity
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.fragment.console.ConsoleViewModel
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.GuideDataStore
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.ShellUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreFragment : Fragment() {

    private lateinit var binding: FragmentRestoreBinding

    private lateinit var viewModel: RestoreViewModel

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var pathUtil: PathUtil

    private lateinit var consoleViewModel: ConsoleViewModel

    lateinit var navHostFragment: NavHostFragment

    lateinit var navController: NavController

    private lateinit var appListDelegate: AppListDelegate

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(RestoreViewModel::class.java)
        binding.viewModel = viewModel
        setHasOptionsMenu(true)

        pathUtil = PathUtil(requireContext())

        consoleViewModel =
            ViewModelProvider(requireActivity()).get(ConsoleViewModel::class.java)

        navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navController = navHostFragment.navController

        appListDelegate = AppListDelegate(requireContext())

        binding.button.setOnClickListener {
            activityResultLauncher.launch(Intent(context, FileActivity::class.java))
        }

        if (!viewModel.isInitialized) {
            binding.button.visibility = View.VISIBLE
            binding.linearLayout.visibility = View.GONE
        } else {
            binding.button.visibility = View.GONE
            binding.linearLayout.visibility = View.VISIBLE
            if (viewModel.backupPath != "") {
                viewModel.isInitialized = false
                val appListFile =
                    ShellUtil.cat(viewModel.backupPath + "/" + pathUtil.APP_LIST_FILE_NAME)
                viewModel.initialize(requireContext(), appListFile, appListDelegate)
            }
        }

        binding.recyclerView.adapter = viewModel.adapter
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager

        binding.textButtonReverse.setOnClickListener {
            val items: Array<String> =
                arrayOf(getString(R.string.reverse_only_app), getString(R.string.reverse_backup))
            var choice = 0
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.reverse_apps))
                .setCancelable(true)
                .setSingleChoiceItems(
                    items,
                    choice
                ) { _, which ->
                    choice = which
                }
                .setPositiveButton(getString(R.string.dialog_positive)) { _, _ ->
                    when (choice) {
                        0 -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                for (i in viewModel.adapter.items as List<AppInfo>) {
                                    i.isOnly = !i.isOnly
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.adapter.notifyDataSetChanged()
                                }
                            }
                        }
                        1 -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                for (i in viewModel.adapter.items as List<AppInfo>) {
                                    i.isSelected = !i.isSelected
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
                .show()
        }

        binding.textButtonRefresh.setOnClickListener {
            if (viewModel.backupPath != "") {
                val refreshCommand =
                    "cd ${viewModel.backupPath}; sh ${viewModel.backupPath}/${pathUtil.DUMP_NAME}; exit"
                toConsoleFragment(refreshCommand)
            }
        }

        binding.filledButtonRestore.setOnClickListener {
            if (viewModel.backupPath != "") {
                generateAppList()
                val restoreCommand =
                    "cd ${viewModel.backupPath}; sh ${viewModel.backupPath}/${pathUtil.RESTORE_BACKUP_NAME}; exit"
                toConsoleFragment(restoreCommand)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val path = it.data?.getStringExtra("path")
                if (path != null) {
                    binding.button.visibility = View.GONE
                    binding.linearLayout.visibility = View.VISIBLE
                    val appListFile =
                        ShellUtil.cat(path + "/" + pathUtil.APP_LIST_FILE_NAME)
                    viewModel.initialize(requireContext(), appListFile, appListDelegate)
                    viewModel.backupPath = path
                }
            }

        CoroutineScope(Dispatchers.IO).launch {
            GuideDataStore.getRestoreGuide(requireContext()).collect {
                if (!it) {
                    Looper.prepare()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.dialog_query_tips))
                        .setMessage(getString(R.string.restore_guide))
                        .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ -> }
                        .setNeutralButton(getString(R.string.igonre)) { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                GuideDataStore.saveRestoreGuide(requireContext(), true)
                            }
                        }
                        .show()
                    Looper.loop()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.topbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        when (item.itemId) {
            R.id.menu_console -> {
                navController.navigate(R.id.action_page_restore_to_page_console)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toConsoleFragment(command: String) {
        if (consoleViewModel.isInitialized) {
            if (consoleViewModel.session.isRunning) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_query_tips))
                    .setMessage(getString(R.string.dialog_query_force_excute))
                    .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
                    .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ ->
                        consoleViewModel.isInitialized = false
                        navController.navigate(
                            RestoreFragmentDirections.actionPageRestoreToPageConsole(command)
                        )
                    }
                    .show()
            } else {
                consoleViewModel.isInitialized = false
                navController.navigate(
                    RestoreFragmentDirections.actionPageRestoreToPageConsole(command)
                )
            }
        } else {
            navController.navigate(
                RestoreFragmentDirections.actionPageRestoreToPageConsole(command)
            )
        }
    }

    private fun generateAppList() {
        CoroutineScope(Dispatchers.IO).launch {
            val appList = viewModel.adapter.items as List<AppInfo>
            var content = ""
            for (i in appList) {
                content +=
                    (if (!i.isSelected) "#" else "") + i.appName.replace(
                        " ",
                        ""
                    ) + (if (i.isOnly) "!" else "") + " " + i.appPackage + " " + i.appType + "\n"
            }
            ShellUtil.writeFile(content, "${viewModel.backupPath}/${pathUtil.APP_LIST_FILE_NAME}")
        }
    }

}