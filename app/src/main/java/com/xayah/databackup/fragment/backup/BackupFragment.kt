package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.fragment.console.ConsoleViewModel
import com.xayah.databackup.model.app.AppDatabase
import com.xayah.databackup.model.app.AppEntity
import com.xayah.databackup.util.GuideDataStore
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SettingsPreferencesDataStore
import com.xayah.databackup.util.ShellUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class BackupFragment : Fragment() {

    companion object {
        fun newInstance() = BackupFragment()
    }

    private lateinit var binding: FragmentBackupBinding

    private lateinit var viewModel: BackupViewModel

    private lateinit var pathUtil: PathUtil

    private lateinit var consoleViewModel: ConsoleViewModel

    private lateinit var db: AppDatabase

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

        viewModel.initialize(requireContext(), appListDelegate) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.recyclerView.adapter = viewModel.adapter
                val layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerView.layoutManager = layoutManager
                delay(1000)
//                TransitionUtil.TransitionX(requireActivity().window.decorView as ViewGroup)
                binding.linearProgressIndicator.visibility = View.GONE
                binding.constraintLayout.visibility = View.VISIBLE
            }
        }


        consoleViewModel.onProcessCompletedListener = {
            CoroutineScope(Dispatchers.IO).launch {
                val appListFile = ShellUtil.cat(pathUtil.APP_LIST_FILE_PATH)
                ShellUtil.rm(pathUtil.APP_LIST_FILE_PATH)
                db.appDao().deleteAll(db.appDao().getAllApps())
                for (i in appListFile) {
                    val info = i.split(" ")
                    if (info.size == 2) {
                        val appEntity =
                            AppEntity(0, info[0].replace("[#\\/:*?\"<>|]".toRegex(), ""), info[1])
                        appEntity.isSelected = !i.contains("#")
                        appEntity.isOnly = i.contains("!")
                        db.appDao().insertAll(appEntity)
                    }
                }
            }
        }

        binding.chipIsOnly.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                db.appDao().selectAllIsOnly(isChecked)
                for (i in viewModel.adapter.items as List<AppEntity>) {
                    i.isOnly = isChecked
                }
                withContext(Dispatchers.Main) {
                    viewModel.adapter.notifyDataSetChanged()
                }
            }
        }

        binding.chipIsSelected.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                db.appDao().selectAllIsSelected(isChecked)
                for (i in viewModel.adapter.items as List<AppEntity>) {
                    i.isSelected = isChecked
                }
                withContext(Dispatchers.Main) {
                    viewModel.adapter.notifyDataSetChanged()
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(string: String): Boolean {
                return false
            }

            override fun onQueryTextChange(string: String): Boolean {
                appListDelegate.filter.filter(string)
                return false
            }
        })

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

    private fun init() {
        viewModel = ViewModelProvider(this).get(BackupViewModel::class.java)
        binding.viewModel = viewModel
        setHasOptionsMenu(true)

        pathUtil = PathUtil(requireContext())

        db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "app").build()

        navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navController = navHostFragment.navController

        appListDelegate = AppListDelegate(requireContext())

        consoleViewModel =
            ViewModelProvider(requireActivity()).get(ConsoleViewModel::class.java)
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
            R.id.menu_refresh -> {
                generateAppList()
                val refreshCommand =
                    "cd ${pathUtil.SCRIPT_PATH}; sh ${pathUtil.GENERATE_APP_LIST_SCRIPT_PATH}; exit"
                SettingsPreferencesDataStore.generateConfigFile(requireContext())
                toConsoleFragment(refreshCommand)
            }
            R.id.menu_confirm -> {
                generateAppList()
                val backupCommand =
                    "cd ${pathUtil.SCRIPT_PATH}; sh ${pathUtil.BACKUP_SCRIPT_PATH}; exit"
                SettingsPreferencesDataStore.generateConfigFile(requireContext())
                toConsoleFragment(backupCommand)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generateAppList() {
        CoroutineScope(Dispatchers.IO).launch {
            val appList = db.appDao().getAllApps()
            var content = ""
            for (i in appList) {
                content +=
                    (if (!i.isSelected) "#" else "") + i.appName.replace(
                        " ",
                        ""
                    ) + (if (i.isOnly) "!" else "") + " " + i.appPackage + "\n"
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