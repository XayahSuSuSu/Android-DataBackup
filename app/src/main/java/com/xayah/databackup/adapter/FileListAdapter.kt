package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.xayah.databackup.R
import com.xayah.databackup.databinding.ActivityFileBinding
import com.xayah.databackup.databinding.AdapterFileBinding
import com.xayah.databackup.model.FileInfo
import com.xayah.databackup.util.ShellUtil


class FileListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<FileListAdapter.Holder>() {
    class Holder(val binding: AdapterFileBinding) : RecyclerView.ViewHolder(binding.root)

    var fileList: MutableList<FileInfo> = mutableListOf()
    var path = mutableListOf<String>("", "storage", "emulated", "0", "Download")
    lateinit var activityBinding: ActivityFileBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterFileBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = fileList[position]
        val binding = holder.binding
        binding.titleView.text = current.name
        if (current.isDir) {
            binding.iconView.background =
                AppCompatResources.getDrawable(mContext, R.drawable.ic_outline_folder)
        } else {
            binding.iconView.background =
                AppCompatResources.getDrawable(mContext, R.drawable.ic_baseline_file)
        }
        binding.content.setOnClickListener {
            if (current.isDir) {
                val dirName = binding.titleView.text
                path.add(dirName.toString())
                fileList = ShellUtil.getFile(pathToString())
                activityBinding.topAppBar.subtitle = pathToString()
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun bind(binding: ActivityFileBinding) {
        this.activityBinding = binding
    }

    fun init() {
        fileList = ShellUtil.getFile(pathToString())
        activityBinding.topAppBar.subtitle = pathToString()
    }

    fun pathToString(): String {
        return path.joinToString(separator = "/")
    }
}