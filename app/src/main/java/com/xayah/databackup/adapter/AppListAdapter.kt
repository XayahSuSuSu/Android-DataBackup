package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.xayah.databackup.R
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.model.AppInfo


class AppListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<AppListAdapter.Holder>(), Filterable {
    class Holder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    var appList: MutableList<AppInfo> = mutableListOf()

    var appListBackup: MutableList<AppInfo> = mutableListOf()

    var isFiltered = false
    var isChanged = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterAppListBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.setIsRecyclable(false)
        val current = appList[position]
        val binding = holder.binding
        binding.adapterIcon.setImageDrawable(current.appIcon)
        binding.adapterName.text = current.appName
        binding.adapterPackage.text = current.appPackage
        binding.adapterBan.isChecked = !current.ban
        binding.adapterOnlyApp.isChecked = current.onlyApp
        if (binding.adapterOnlyApp.isChecked) {
            binding.materialCardView.setCardBackgroundColor(
                mContext.getColor(R.color.color_light_background_white)
            )
        } else {
            binding.materialCardView.setCardBackgroundColor(
                mContext.getColor(R.color.color_light_background_shallow)
            )
        }

        binding.adapterBan.setOnCheckedChangeListener { _, _ ->
            isChanged = true
            appList[position].ban = !appList[position].ban
        }
        binding.adapterOnlyApp.setOnCheckedChangeListener { _, _ ->
            isChanged = true
            appList[position].onlyApp = !appList[position].onlyApp
            if (binding.adapterOnlyApp.isChecked) {
                binding.materialCardView.setCardBackgroundColor(
                    mContext.getColor(R.color.color_light_background_white)
                )
            } else {
                binding.materialCardView.setCardBackgroundColor(
                    mContext.getColor(R.color.color_light_background_shallow)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    fun addApp(appInfo: AppInfo) {
        appList.add(appInfo)
        notifyItemInserted(0)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun reverseOnlyApp(position: Int) {
        appList[position].onlyApp = !appList[position].onlyApp
        notifyItemChanged(position)
    }

    fun reverseBan(position: Int) {
        appList[position].ban = !appList[position].ban
        notifyItemChanged(position)
    }

    fun selectAll(type: Int, state: Int) {
        if (type == 0) {
            for (i in appList)
                i.onlyApp = state == 1
            notifyDataSetChanged()
        } else {
            for (i in appList)
                i.ban = state == 0
            notifyDataSetChanged()
        }

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                if (!isFiltered) {
                    appListBackup = appList
                }
                if (constraint!!.isEmpty()) {
                    appList = appListBackup
                } else {
                    val tmp: MutableList<AppInfo> = mutableListOf()
                    for (i in appListBackup) {
                        if (i.appName.contains(constraint, true)) {
                            tmp.add(i)
                        }
                    }
                    appList = tmp
                }
                val filterResults = FilterResults()
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                isFiltered = true
                notifyDataSetChanged()
            }

        }
    }
}