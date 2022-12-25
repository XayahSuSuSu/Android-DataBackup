package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.data.AppInfo
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.view.util.dp

abstract class AppListAdapterBase : ItemViewDelegate<AppInfo, AppListAdapterBase.ViewHolder>() {
    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    abstract fun onChipClickInvoke()

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfo) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.appIcon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.packageName

        // 底部卡片边距设定
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

        // 版本Chip
        binding.chipVersion.apply {
            visibility = if (item.backup.versionName.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
            text = item.backup.versionName
            setOnClickListener {
                Toast.makeText(context, item.backup.versionName, Toast.LENGTH_SHORT).show()
            }
        }

        // 日期Chip
        binding.chipDate.apply {
            if (item.restoreList.isNotEmpty()) {
                visibility = View.VISIBLE
                text = Command.getDate(item.restoreList[item.restoreIndex].date)
            } else {
                visibility = View.GONE
            }
        }
    }
}