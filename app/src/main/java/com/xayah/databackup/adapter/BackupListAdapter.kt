package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xayah.databackup.databinding.AdapterBackupListBinding
import com.xayah.databackup.model.BackupInfo


class BackupListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<BackupListAdapter.Holder>() {
    class Holder(val binding: AdapterBackupListBinding) : RecyclerView.ViewHolder(binding.root)

    var backupList: MutableList<BackupInfo> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterBackupListBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    var chosenIndex = 0
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = backupList[position]
        val binding = holder.binding
        binding.adapterName.text = current.backupName
        binding.adapterTime.text = current.backupTime
        binding.adapterCheck.isChecked = (position == chosenIndex)
        binding.adapterCheck.setOnClickListener {
            chosenIndex = position
            notifyItemRangeChanged(0, backupList.size)
        }
    }

    override fun getItemCount(): Int {
        return backupList.size
    }

    fun addBackup(backupInfo: BackupInfo) {
        backupList.add(backupInfo)
        notifyItemInserted(0)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}