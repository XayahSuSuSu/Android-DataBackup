package com.xayah.databackup.adapter

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.util.Command
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

        binding.iconButton.visibility = View.VISIBLE
        binding.iconButton.setOnClickListener {
            MaterialAlertDialogBuilder(context).apply {
                setWithConfirm(context.getString(R.string.delete_confirm)) {
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
                                context.getString(com.xayah.design.R.string.failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}