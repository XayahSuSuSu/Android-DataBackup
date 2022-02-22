package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.model.AppInfo

class AppListDelegate(val mContext: Context) :
    ItemViewDelegate<AppInfo, AppListDelegate.ViewHolder>(), Filterable {

    var appList: MutableList<AppInfo> = mutableListOf()

    var isFiltered = false

    var isAttached = false

    var isRestore = false

    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: ViewHolder, item: AppInfo) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.appIcon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.appPackage
        binding.isSelected.isChecked = item.isSelected
        binding.isOnly.isChecked = item.isOnly

        binding.isOnly.setOnCheckedChangeListener { _, isChecked ->
            (adapterItems[holder.bindingAdapterPosition] as AppInfo).isOnly = isChecked
        }

        binding.isSelected.setOnCheckedChangeListener { _, isChecked ->
            (adapterItems[holder.bindingAdapterPosition] as AppInfo).isSelected = isChecked
        }
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                if (!isFiltered) {
                    appList = adapterItems as MutableList<AppInfo>
                }
                if (constraint!!.isEmpty()) {
                    adapterItems = appList
                } else {
                    val tmp: MutableList<AppInfo> = mutableListOf()
                    for (i in appList) {
                        if (i.appName.contains(constraint, true)) {
                            tmp.add(i)
                        }
                    }
                    adapterItems = tmp
                }
                return FilterResults()
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                isFiltered = true
                if (isAttached)
                    adapter.notifyDataSetChanged()
            }

        }
    }
}