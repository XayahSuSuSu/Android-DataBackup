package com.xayah.databackup.adapter

import android.content.Context
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.util.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapterBackup(private val room: Room?, override val context: Context) :
    AppListAdapterBase(context) {

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        super.onBindViewHolder(holder, item)
        val binding = holder.binding

        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                if (!item.isProcessing) {
                    (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupApp = checked
                    if (room != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            room.findByPackage(item.packageName) {
                                it.backupApp = checked
                                room.update(it)
                            }
                        }
                    }
                }
            }
            isChecked = item.backupApp
        }
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                if (!item.isProcessing) {
                    (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupData = checked
                    if (room != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            room.findByPackage(item.packageName) {
                                it.backupData = checked
                                room.update(it)
                            }
                        }
                    }
                }
            }
            isChecked = item.backupData
        }
    }
}