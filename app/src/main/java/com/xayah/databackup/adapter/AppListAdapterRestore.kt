package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.setLoading
import com.xayah.databackup.view.util.dp
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListAdapterRestore(private val mAppInfoRestoreList: MutableList<AppInfoRestore>) :
    ItemViewDelegate<AppInfoRestore, AppListAdapterRestore.ViewHolder>() {
    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfoRestore) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(
            if (item.infoBase.appIcon == null) AppCompatResources.getDrawable(
                binding.root.context, R.drawable.ic_round_android
            ) else item.infoBase.appIcon
        )
        binding.appName.text = item.infoBase.appName
        binding.appPackage.text = item.infoBase.packageName

        if (holder.bindingAdapterPosition == adapterItems.size - 1) {
            binding.materialCardView.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16.dp
                    bottomMargin = 16.dp
                    marginStart = 20.dp
                    marginEnd = 20.dp
                }
            }
        } else {
            binding.materialCardView.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16.dp
                    marginStart = 20.dp
                    marginEnd = 20.dp
                }
            }
        }
        // ----------------------------------------------------------------------------------------
        binding.iconButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                MaterialAlertDialogBuilder(context).apply {
                    setWithConfirm("${GlobalString.confirmRemove}${GlobalString.symbolQuestion}") {
                        CoroutineScope(Dispatchers.IO).launch {
                            Command.rm("${Path.getBackupDataSavePath()}/${item.infoBase.packageName}")
                                .apply {
                                    val that = this
                                    withContext(Dispatchers.Main) {
                                        if (that) {
                                            val items = adapterItems.toMutableList()
                                            items.remove(item)
                                            mAppInfoRestoreList.remove(item)
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
        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoRestore).infoBase.app =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.app
        }
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoRestore).infoBase.data =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.data
        }
        if (!item.hasApp) {
            binding.chipApplication.apply {
                isChecked = false
                isEnabled = false
            }
        }
        if (!item.hasData) {
            binding.chipData.apply {
                isChecked = false
                isEnabled = false
            }
        }
    }
}