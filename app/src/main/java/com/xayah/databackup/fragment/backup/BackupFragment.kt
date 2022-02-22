package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.fragment.console.ConsoleViewModel
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.GuideDataStore
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SettingsPreferencesDataStore
import com.xayah.databackup.util.ShellUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class BackupFragment : Fragment() {
    private lateinit var binding: FragmentBackupBinding

    private lateinit var viewModel: BackupViewModel

    private lateinit var pathUtil: PathUtil

    private lateinit var consoleViewModel: ConsoleViewModel

    private lateinit var appListDelegate: AppListDelegate

    lateinit var navHostFragment: NavHostFragment

    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        init()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(string: String): Boolean {
                return false
            }

            override fun onQueryTextChange(string: String): Boolean {
                appListDelegate.filter.filter(string)
                return false
            }
        })

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
            generateAppList()
            val refreshCommand =
                "cd ${pathUtil.SCRIPT_PATH}; sh ${pathUtil.GENERATE_APP_LIST_SCRIPT_PATH}; exit"
            SettingsPreferencesDataStore.generateConfigFile(requireContext())
            toConsoleFragment(refreshCommand)
        }

        binding.filledButtonBackup.setOnClickListener {
            generateAppList()
            val backupCommand =
                "cd ${pathUtil.SCRIPT_PATH}; sh ${pathUtil.BACKUP_SCRIPT_PATH}; exit"
            SettingsPreferencesDataStore.generateConfigFile(requireContext())
            toConsoleFragment(backupCommand)
        }


    }

    private fun init() {
        viewModel = ViewModelProvider(this).get(BackupViewModel::class.java)
        binding.viewModel = viewModel
        setHasOptionsMenu(true)

        pathUtil = PathUtil(requireContext())

        navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navController = navHostFragment.navController

        appListDelegate = AppListDelegate(requireContext())

        consoleViewModel =
            ViewModelProvider(requireActivity()).get(ConsoleViewModel::class.java)

        val appListFile = ShellUtil.cat(pathUtil.APP_LIST_FILE_PATH)
        viewModel.initialize(requireContext(), appListFile, appListDelegate) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.recyclerView.adapter = viewModel.adapter
                val layoutManager = GridLayoutManager(requireContext(), 1)
                binding.recyclerView.layoutManager = layoutManager
                delay(1000)
//                TransitionUtil.TransitionX(requireActivity().window.decorView as ViewGroup)
                binding.linearProgressIndicator.visibility = View.GONE
                binding.constraintLayout.visibility = View.VISIBLE
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            GuideDataStore.getBackupGuide(requireContext()).collect {
                if (!it) {
                    Looper.prepare()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.dialog_query_tips))
                        .setMessage(getString(R.string.backup_guide))
                        .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ -> }
                        .setNeutralButton(getString(R.string.igonre)) { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                GuideDataStore.saveBackupGuide(requireContext(), true)
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
        when (item.itemId) {
            R.id.menu_console -> {
                navController.navigate(BackupFragmentDirections.actionPageBackupToPageConsole())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generateAppList() {
        CoroutineScope(Dispatchers.IO).launch {
            val appList = appListDelegate.adapterItems as MutableList<AppInfo>
            var content = ""
            for (i in appList) {
                content +=
                    (if (!i.isSelected) "#" else "") + i.appName.replace(
                        " ",
                        ""
                    ) + (if (i.isOnly) "!" else "") + " " + i.appPackage + " " + i.appType + "\n"
            }
            ShellUtil.writeFile(content, pathUtil.APP_LIST_FILE_PATH)
        }
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
                            BackupFragmentDirections.actionPageBackupToPageConsole(command)
                        )
                    }
                    .show()
            } else {
                consoleViewModel.isInitialized = false
                navController.navigate(
                    BackupFragmentDirections.actionPageBackupToPageConsole(command)
                )
            }
        } else {
            navController.navigate(
                BackupFragmentDirections.actionPageBackupToPageConsole(command)
            )
        }
    }
}