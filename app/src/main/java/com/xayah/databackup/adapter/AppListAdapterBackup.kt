package com.xayah.databackup.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.data.AppInfoBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapterBackup(val onChipClick: () -> Unit = {}) :
    ItemViewDelegate<AppInfoBackup, AppListAdapterBase.ViewHolder>() {

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup
    ): AppListAdapterBase.ViewHolder {
        return AppListAdapterBase.onCreateViewHolder(context, parent)
    }

    override fun onBindViewHolder(holder: AppListAdapterBase.ViewHolder, item: AppInfoBackup) {
        AppListAdapterBase.onBindViewHolder(holder, item, adapterItems)

        val binding = holder.binding
        val adapterItem = (adapterItems[holder.bindingAdapterPosition] as AppInfoBackup)
        // 应用Chip
        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                adapterItem.detailBackup.selectApp =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                    onChipClick()
                }
            }
            isChecked = item.detailBackup.selectApp
        }

        // 数据Chip
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                adapterItem.detailBackup.selectData =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                    onChipClick()
                }
            }
            isChecked = item.detailBackup.selectData
        }

        // 版本Chip
        binding.chipVersion.apply {
            visibility = if (item.detailBackup.versionName.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
            text = item.detailBackup.versionName
            setOnClickListener {
                Toast.makeText(context, item.detailBackup.versionName, Toast.LENGTH_SHORT).show()
            }
        }

        // 存储Chip
        binding.chipSize.apply {
            if (item.storageStats.sizeBytes != 0L) {
                visibility = View.VISIBLE
                text = item.storageStats.sizeDisplay
            } else {
                visibility = View.GONE
            }
        }
    }
}