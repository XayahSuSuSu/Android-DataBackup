package com.xayah.databackup.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.graphics.drawable.toDrawable
import com.drakeet.multitype.ItemViewDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListAdapterRestore(val onChipClick: () -> Unit = {}) :
    ItemViewDelegate<AppInfoRestore, AppListAdapterBase.ViewHolder>() {

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup
    ): AppListAdapterBase.ViewHolder {
        return AppListAdapterBase.onCreateViewHolder(context, parent)
    }

    override fun onBindViewHolder(holder: AppListAdapterBase.ViewHolder, item: AppInfoRestore) {
        AppListAdapterBase.onBindViewHolder(holder, item, adapterItems)

        val binding = holder.binding
        val adapterItem = (adapterItems[holder.bindingAdapterPosition] as AppInfoRestore)

        if (adapterItem.detailRestoreList.isNotEmpty()) {
            binding.materialCardView.setOnClickListener {
                if ((item.detailRestoreList[item.restoreIndex].selectApp && item.detailRestoreList[item.restoreIndex].selectData).not() &&
                    (item.detailRestoreList[item.restoreIndex].selectApp || item.detailRestoreList[item.restoreIndex].selectData)
                ) {
                    if (item.detailRestoreList[item.restoreIndex].selectApp.not()) {
                        binding.chipApplication.performClick()
                    } else {
                        binding.chipData.performClick()
                    }
                } else {
                    binding.chipApplication.performClick()
                    binding.chipData.performClick()
                }
            }
        }

        // 应用图标
        binding.appIcon.setImageDrawable(
            if (adapterItem.detailBase.appIcon == null) AppCompatResources.getDrawable(
                binding.root.context, R.drawable.ic_round_android
            ) else adapterItem.detailBase.appIcon
        )

        SafeFile.create("${Path.getBackupDataSavePath()}/${item.detailBase.packageName}/icon.png") {
            it.apply {
                val bytes = readBytes()
                binding.appIcon.setImageDrawable(
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        .toDrawable(binding.root.context.resources)
                )
            }
        }

        // 应用Chip
        binding.chipApplication.apply {
            if (adapterItem.detailRestoreList.isNotEmpty()) {
                setOnCheckedChangeListener { _, checked ->
                    adapterItem.detailRestoreList[adapterItem.restoreIndex].selectApp =
                        checked
                    CoroutineScope(Dispatchers.Main).launch {
                        adapter.notifyItemChanged(holder.bindingAdapterPosition)
                        onChipClick()
                    }
                }
                isChecked = adapterItem.detailRestoreList[adapterItem.restoreIndex].selectApp
                isEnabled = adapterItem.detailRestoreList[adapterItem.restoreIndex].hasApp
            }

        }

        // 数据Chip
        binding.chipData.apply {
            if (adapterItem.detailRestoreList.isNotEmpty()) {
                setOnCheckedChangeListener { _, checked ->
                    adapterItem.detailRestoreList[adapterItem.restoreIndex].selectData =
                        checked
                    CoroutineScope(Dispatchers.Main).launch {
                        adapter.notifyItemChanged(holder.bindingAdapterPosition)
                        onChipClick()
                    }
                }
                isChecked = adapterItem.detailRestoreList[adapterItem.restoreIndex].selectData
                isEnabled = adapterItem.detailRestoreList[adapterItem.restoreIndex].hasData
            }
        }

        // 存储Chip
        binding.chipSize.apply {
            if (item.detailRestoreList[item.restoreIndex].sizeBytes != 0L) {
                visibility = View.VISIBLE
                text = item.detailRestoreList[item.restoreIndex].sizeDisplay
            } else {
                visibility = View.GONE
            }
        }

        // 日期Chip
        binding.chipDate.apply {
            if (item.detailRestoreList.isNotEmpty()) {
                visibility = View.VISIBLE
                text = Command.getDate(item.detailRestoreList[item.restoreIndex].date)
            } else {
                visibility = View.GONE
            }

            setOnClickListener {
                val choice = adapterItem.restoreIndex
                val items = mutableListOf<String>()
                adapterItem.detailRestoreList.forEach { items.add(Command.getDate(it.date)) }

                ListPopupWindow(context).apply {
                    fastInitialize(binding.chipDate, items.toTypedArray(), choice)
                    setOnItemClickListener { _, _, position, _ ->
                        dismiss()
                        adapterItem.restoreIndex = position
                        adapter.notifyItemChanged(holder.bindingAdapterPosition)
                    }
                    show()
                }
            }
        }

        // 删除按钮
        binding.iconButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                MaterialAlertDialogBuilder(context).apply {
                    setWithConfirm("${GlobalString.removeSelectedBackupFiles}${GlobalString.symbolQuestion}") {
                        CoroutineScope(Dispatchers.IO).launch {
                            Command.rm("${Path.getBackupDataSavePath()}/${item.detailBase.packageName}/${item.detailRestoreList[item.restoreIndex].date}")
                                .apply {
                                    val that = this
                                    withContext(Dispatchers.Main) {
                                        if (that) {
                                            item.detailRestoreList.remove(
                                                item.detailRestoreList[item.restoreIndex]
                                            )
                                            item.restoreIndex--
                                            if (item.detailRestoreList.isNotEmpty()) {
                                                adapter.notifyItemChanged(holder.bindingAdapterPosition)
                                            } else {
                                                GlobalObject.getInstance().appInfoRestoreMap.value.remove(
                                                    item.detailBase.packageName
                                                )
                                                val items = adapterItems.toMutableList()
                                                items.remove(item)
                                                adapterItems = items.toList()
                                                adapter.notifyItemRemoved(holder.bindingAdapterPosition)
                                            }
                                            Toast.makeText(
                                                context,
                                                GlobalString.success,
                                                Toast.LENGTH_SHORT
                                            ).show()
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
        }
    }
}