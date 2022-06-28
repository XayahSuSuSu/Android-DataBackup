package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.google.android.material.chip.Chip
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding

open class AppListHeaderAdapterBase(
    val onInitialize: (binding: AdapterAppListHeaderBinding) -> Unit,
    val onChipAppClick: (v: Chip) -> Unit,
    val onChipDataClick: (v: Chip) -> Unit,
) : ItemViewDelegate<String, AppListHeaderAdapterBase.ViewHolder>() {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListHeaderBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: String) {
        val binding = holder.binding

        onInitialize(binding)

        binding.chipApp.setOnClickListener {
            onChipAppClick(binding.chipApp)
        }
        binding.chipData.setOnClickListener {
            onChipDataClick(binding.chipData)
        }
    }

    class ViewHolder(val binding: AdapterAppListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)
}