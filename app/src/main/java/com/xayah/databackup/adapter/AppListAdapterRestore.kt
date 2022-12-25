package com.xayah.databackup.adapter

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfo
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.setLoading
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListAdapterRestore(
    val onChipClick: () -> Unit = {}
) : AppListAdapterBase() {

    override fun onChipClickInvoke() {
        onChipClick()
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfo) {
        super.onBindViewHolder(holder, item)

        val binding = holder.binding
        val adapterItem = (adapterItems[holder.bindingAdapterPosition] as AppInfo)

        // 应用图标
        binding.appIcon.setImageDrawable(
            if (adapterItem.appIcon == null) AppCompatResources.getDrawable(
                binding.root.context, R.drawable.ic_round_android
            ) else adapterItem.appIcon
        )
        adapterItem.appIconString?.apply {
            if (this.isNotEmpty()) {
                try {
                    val img = Base64.decode(this.toByteArray(), Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(img, 0, img.size)
                    val drawable: Drawable = BitmapDrawable(App.globalContext.resources, bitmap)
                    binding.appIcon.setImageDrawable(drawable)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        // 应用Chip
        binding.chipApplication.apply {
            if (adapterItem.restoreList.isNotEmpty()) {
                setOnCheckedChangeListener { _, checked ->
                    adapterItem.restoreList[adapterItem.restoreIndex].app =
                        checked
                    CoroutineScope(Dispatchers.Main).launch {
                        adapter.notifyItemChanged(0)
                        onChipClickInvoke()
                    }
                }
                isChecked = adapterItem.restoreList[adapterItem.restoreIndex].app
                isEnabled = adapterItem.restoreList[adapterItem.restoreIndex].hasApp
            }

        }

        // 数据Chip
        binding.chipData.apply {
            if (adapterItem.restoreList.isNotEmpty()) {
                setOnCheckedChangeListener { _, checked ->
                    adapterItem.restoreList[adapterItem.restoreIndex].data =
                        checked
                    CoroutineScope(Dispatchers.Main).launch {
                        adapter.notifyItemChanged(0)
                        onChipClickInvoke()
                    }
                }
                isChecked = adapterItem.restoreList[adapterItem.restoreIndex].data
                isEnabled = adapterItem.restoreList[adapterItem.restoreIndex].hasData
            }
        }

        // 日期Chip
        binding.chipDate.apply {
            setOnClickListener {
                val choice = adapterItem.restoreIndex
                val items = mutableListOf<String>()
                adapterItem.restoreList.forEach { items.add(Command.getDate(it.date)) }

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
                    setWithConfirm("${GlobalString.confirmRemove}${GlobalString.symbolQuestion}") {
                        CoroutineScope(Dispatchers.IO).launch {
                            Command.rm("${Path.getBackupDataSavePath()}/${item.packageName}")
                                .apply {
                                    val that = this
                                    withContext(Dispatchers.Main) {
                                        if (that) {
                                            val items = adapterItems.toMutableList()
                                            items.remove(item)
                                            App.appInfoList.value.remove(item)
                                            adapterItems = items.toList()
                                            BottomSheetDialog(context).apply {
                                                setLoading()
                                                val dialog = this
                                                withContext(Dispatchers.Main) {
                                                    dialog.dismiss()
                                                    adapter.notifyItemRemoved(holder.bindingAdapterPosition)
                                                    Toast.makeText(
                                                        context,
                                                        GlobalString.success,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                GlobalString.failed,
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
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