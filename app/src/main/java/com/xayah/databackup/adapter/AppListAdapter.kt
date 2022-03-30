package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.dp

class AppListAdapter : ItemViewDelegate<AppInfo, AppListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfo) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.icon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.packageName
        if (holder.bindingAdapterPosition == 0) {
            binding.materialCardView.apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                        .apply {
                            topMargin = 16.dp
                            bottomMargin = 16.dp
                            marginStart = 20.dp
                            marginEnd = 20.dp
                        }
            }
        }
    }

    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)
}