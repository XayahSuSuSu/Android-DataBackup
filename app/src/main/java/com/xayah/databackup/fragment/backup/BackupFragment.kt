package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Room

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
        viewModel.initialize(mContext, room) { setHasOptionsMenu(true) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.backup, menu)
        viewModel.setSearchView(requireActivity(), menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val mContext = requireActivity()
        when (item.itemId) {
            R.id.backup_reverse -> {
                viewModel.setReverse(mContext, room)
            }
            R.id.backup_confirm -> {
                viewModel.setConfirm(mContext) { setHasOptionsMenu(false) }
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