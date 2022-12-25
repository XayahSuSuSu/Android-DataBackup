package com.xayah.databackup.adapter

import com.xayah.databackup.data.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapterBackup(
    val onChipClick: () -> Unit = {}
) : AppListAdapterBase() {

    override fun onChipClickInvoke() {
        onChipClick()
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfo) {
        super.onBindViewHolder(holder, item)

        val binding = holder.binding
        val adapterItem = (adapterItems[holder.bindingAdapterPosition] as AppInfo)
        // 应用Chip
        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                adapterItem.backup.app =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                    onChipClickInvoke()
                }
            }
            isChecked = item.backup.app
        }

        // 数据Chip
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                adapterItem.backup.data =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                    onChipClickInvoke()
                }
            }
            isChecked = item.backup.data
        }
    }
}