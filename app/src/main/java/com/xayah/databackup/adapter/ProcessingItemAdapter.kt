package com.xayah.databackup.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.databinding.AdapterProcessingItemBinding

class ProcessingItemAdapter : ItemViewDelegate<ProcessingItem, ProcessingItemAdapter.ViewHolder>() {
    class ViewHolder(val binding: AdapterProcessingItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterProcessingItemBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: ProcessingItem) {
        val binding = holder.binding
        if (item.isProcessing) {
            binding.circularProgressIndicator.visibility = View.VISIBLE
            binding.type.visibility = View.GONE
        } else {
            binding.circularProgressIndicator.visibility = View.GONE
            binding.type.visibility = View.VISIBLE
        }
        binding.type.text = item.type
        binding.title.text = item.title
        binding.subtitle.text = item.subtitle

        var tintColor = App.globalContext.getColor(R.color.red)
        var backgroundColor = App.globalContext.getColor(R.color.redContainer)
        var icon = R.drawable.ic_round_android
        when (item.type) {
            ProcessingItemTypeAPK -> {
                tintColor = App.globalContext.getColor(R.color.red)
                backgroundColor = App.globalContext.getColor(R.color.redContainer)
                icon = R.drawable.ic_round_android
            }
            ProcessingItemTypeUSER -> {
                tintColor = App.globalContext.getColor(R.color.yellow)
                backgroundColor = App.globalContext.getColor(R.color.yellowContainer)
                icon = R.drawable.ic_round_person
            }
            ProcessingItemTypeUSERDE -> {
                tintColor = App.globalContext.getColor(R.color.purple)
                backgroundColor = App.globalContext.getColor(R.color.purpleContainer)
                icon = R.drawable.ic_round_person
            }
            ProcessingItemTypeDATA -> {
                tintColor = App.globalContext.getColor(R.color.green)
                backgroundColor = App.globalContext.getColor(R.color.greenContainer)
                icon = R.drawable.ic_round_database
            }
            ProcessingItemTypeOBB -> {
                tintColor = App.globalContext.getColor(R.color.blue)
                backgroundColor = App.globalContext.getColor(R.color.blueContainer)
                icon = R.drawable.ic_round_esports
            }
        }

        binding.type.setTextColor(tintColor)

        binding.typeIcon.apply {
            imageTintList = ColorStateList.valueOf(tintColor)
            setBackgroundColor(backgroundColor)
            setImageResource(icon)
        }

    }
}