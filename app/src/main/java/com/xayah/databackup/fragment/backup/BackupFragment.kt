package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.xayah.databackup.R
import com.xayah.databackup.databinding.FragmentBackupBinding

class BackupFragment : Fragment() {

    companion object {
        fun newInstance() = BackupFragment()
    }

    private lateinit var binding: FragmentBackupBinding

    private lateinit var viewModel: BackupViewModel

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
                navController.navigate(R.id.action_page_backup_to_page_console)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}