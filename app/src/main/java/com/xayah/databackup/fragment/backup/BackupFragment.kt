package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.fragment.console.ConsoleViewModel
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.TransitionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupFragment : Fragment() {

    companion object {
        fun newInstance() = BackupFragment()
    }

    private lateinit var binding: FragmentBackupBinding

    private lateinit var viewModel: BackupViewModel

    private lateinit var pathUtil: PathUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BackupViewModel::class.java)
        binding.viewModel = viewModel
        setHasOptionsMenu(true)

        pathUtil = PathUtil(requireContext())

        viewModel.initialize(requireContext(), pathUtil.APP_LIST_FILE_PATH) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.recyclerView.adapter = viewModel.adapter
                val layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerView.layoutManager = layoutManager
                TransitionUtil.TransitionX(requireActivity().window.decorView as ViewGroup)
                binding.linearProgressIndicator.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
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
                navController.navigate(BackupFragmentDirections.actionPageBackupToPageConsole())
            }
            R.id.menu_refresh -> {
                val refreshCommand =
                    "cd ${pathUtil.SCRIPT_PATH}; sh ${pathUtil.SCRIPT_PATH}/${pathUtil.GENERATE_APP_LIST_SCRIPT_NAME}; exit"
                val consoleViewModel =
                    ViewModelProvider(requireActivity()).get(ConsoleViewModel::class.java)
                if (consoleViewModel.isInitialized) {
                    if (consoleViewModel.session.isRunning) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_query_tips))
                            .setMessage(getString(R.string.dialog_query_force_excute))
                            .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
                            .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ ->
                                consoleViewModel.isInitialized = false
                                navController.navigate(
                                    BackupFragmentDirections.actionPageBackupToPageConsole(
                                        refreshCommand
                                    )
                                )
                            }
                            .show()
                    } else {
                        consoleViewModel.isInitialized = false
                        navController.navigate(
                            BackupFragmentDirections.actionPageBackupToPageConsole(
                                refreshCommand
                            )
                        )
                    }
                } else {
                    navController.navigate(
                        BackupFragmentDirections.actionPageBackupToPageConsole(
                            refreshCommand
                        )
                    )
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}