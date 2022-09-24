package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.view.util.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapterBackup : ItemViewDelegate<AppInfoBackup, AppListAdapterBackup.ViewHolder>() {
    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfoBackup) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.infoBase.appIcon)
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
        binding.chipApplication.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoBackup).infoBase.app =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.app
        }
        binding.chipData.apply {
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppInfoBackup).infoBase.data =
                    checked
                CoroutineScope(Dispatchers.Main).launch {
                    adapter.notifyItemChanged(0)
                }
            }
            isChecked = item.infoBase.data
        }
    }
}