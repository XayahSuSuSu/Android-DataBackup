package com.xayah.databackup.fragment.restore

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.xayah.databackup.FileActivity
import com.xayah.databackup.R
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.model.app.AppEntity

class RestoreFragment : Fragment() {

    companion object {
        fun newInstance() = RestoreFragment()
    }

    private lateinit var binding: FragmentRestoreBinding

    private lateinit var viewModel: RestoreViewModel

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

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

        binding.button.setOnClickListener {
            activityResultLauncher.launch(Intent(context, FileActivity::class.java))
        }

        if (!viewModel.isInitialized && viewModel.appEntityList.size == 0) {
            binding.button.visibility = View.VISIBLE
            binding.linearLayout.visibility = View.GONE
        } else {
            binding.button.visibility = View.GONE
            binding.linearLayout.visibility = View.VISIBLE
        }

        binding.recyclerView.adapter = viewModel.adapter
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager

        binding.chipIsOnly.setOnCheckedChangeListener { _, isChecked ->
            for (i in viewModel.adapter.items as List<AppEntity>) {
                i.isOnly = isChecked
            }
            viewModel.adapter.notifyDataSetChanged()
        }

        binding.chipIsSelected.setOnCheckedChangeListener { _, isChecked ->
            for (i in viewModel.adapter.items as List<AppEntity>) {
                i.isSelected = isChecked
            }
            viewModel.adapter.notifyDataSetChanged()
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
                    viewModel.initialize(requireContext(), path)
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

}