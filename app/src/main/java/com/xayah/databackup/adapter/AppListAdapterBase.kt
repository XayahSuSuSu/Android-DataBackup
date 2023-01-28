package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfoBase
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.view.util.dp

class AppListAdapterBase {
    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
            return ViewHolder(
                AdapterAppListBinding.inflate(
                    LayoutInflater.from(context), parent, false
                )
            )
        }

        fun onBindViewHolder(holder: ViewHolder, item: AppInfoBase, adapterItems: List<Any>) {
            val binding = holder.binding
            // 应用图标
            binding.appIcon.setImageDrawable(
                if (item.detailBase.appIcon == null) AppCompatResources.getDrawable(
                    binding.root.context, R.drawable.ic_round_android
                ) else item.detailBase.appIcon
            )

            binding.appName.text = item.detailBase.appName
            binding.appPackage.text = item.detailBase.packageName

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
        }
    }
}