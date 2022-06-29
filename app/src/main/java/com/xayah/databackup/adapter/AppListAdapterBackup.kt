package com.xayah.databackup.adapter

import com.xayah.databackup.data.AppInfoBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapterBackup : AppListAdapterBase() {

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfoBackup) {
        super.onBindViewHolder(holder, item)
        val binding = holder.binding

        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoBackup).infoBase.app = checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.app
        }
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoBackup).infoBase.data = checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.data
        }
    }
}