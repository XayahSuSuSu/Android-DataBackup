package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.databinding.AdapterProcessingTaskBinding
import com.xayah.databackup.util.GlobalString

class ProcessingTaskAdapter : ItemViewDelegate<ProcessingTask, ProcessingTaskAdapter.ViewHolder>() {
    class ViewHolder(val binding: AdapterProcessingTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterProcessingTaskBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: ProcessingTask) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(
            if (item.appIcon == null) AppCompatResources.getDrawable(
                binding.root.context, R.drawable.ic_round_android
            ) else item.appIcon
        )
        binding.appName.text = item.appName
        binding.appType.setTextColor(App.globalContext.getColor(R.color.red))

        if (item.app) binding.appType.text = GlobalString.application
        if (item.data) binding.appType.text = GlobalString.data
        if (item.app && item.data) {
            binding.appType.apply {
                text = GlobalString.all
                setTextColor(App.globalContext.getColor(R.color.green))
            }
        }
    }
}