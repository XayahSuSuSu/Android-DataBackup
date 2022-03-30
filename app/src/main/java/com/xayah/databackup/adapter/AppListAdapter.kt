package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.util.Room
import com.xayah.databackup.util.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListAdapter(private val room: Room) :
    ItemViewDelegate<AppEntity, AppListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.icon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.packageName
        binding.chipApplication.apply {
            isChecked = item.backupApp
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupApp = checked
                CoroutineScope(Dispatchers.IO).launch {
                    room.findByPackage(item.packageName) {
                        it.backupApp = checked
                        room.update(it)
                    }
                }
            }
        }
        binding.chipData.apply {
            isChecked = item.backupData
            setOnCheckedChangeListener { _, checked ->
                (adapterItems[holder.bindingAdapterPosition] as AppEntity).backupData = checked
                CoroutineScope(Dispatchers.IO).launch {
                    room.findByPackage(item.packageName) {
                        it.backupData = checked
                        room.update(it)
                    }
                }
            }
        }
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