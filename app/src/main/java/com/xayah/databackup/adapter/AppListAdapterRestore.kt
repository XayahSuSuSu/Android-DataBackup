package com.xayah.databackup.adapter

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.design.view.setWithConfirm

class AppListAdapterRestore(
    private val appList: MutableList<AppEntity>,
    val onEmpty: () -> Unit,
    override val context: Context
) :
    AppListAdapterBase(context) {

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        super.onBindViewHolder(holder, item)
        val binding = holder.binding

        // 检测是否存在安装包
        if (!Command.ls("${item.backupPath}/apk.tar*")) {
            binding.chipApplication.isChecked = false
            item.backupApp = false
            binding.chipApplication.isEnabled = false
        }

        // 检测是否存在数据
        if (!Command.ls("${item.backupPath}/user.tar*") &&
            !Command.ls("${item.backupPath}/data.tar*") &&
            !Command.ls("${item.backupPath}/obb.tar*")
        ) {
            binding.chipData.isChecked = false
            item.backupData = false
            binding.chipData.isEnabled = false
        }

        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                if (!item.isProcessing) {
                    (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupApp = checked
                }
            }
            isChecked = item.backupApp
        }
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                if (!item.isProcessing) {
                    (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupData = checked
                }
            }
            isChecked = item.backupData
        }

        binding.iconButton.visibility = View.VISIBLE
        binding.iconButton.setOnClickListener {
            MaterialAlertDialogBuilder(context).apply {
                setWithConfirm(GlobalString.deleteConfirm) {
                    if (item.backupPath.isNotEmpty()) {
                        val ret = Command.rm(item.backupPath)
                        if (ret) {
                            appList.remove(item)
                            (adapterItems as MutableList<*>).remove(item)
                            adapter.notifyItemRemoved(holder.bindingAdapterPosition)
                            if ((adapterItems as MutableList<*>).isEmpty()) {
                                onEmpty()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                GlobalString.failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}