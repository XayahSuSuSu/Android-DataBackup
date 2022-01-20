package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.model.app.AppDatabase
import com.xayah.databackup.model.app.AppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListDelegate(val mContext: Context) :
    ItemViewDelegate<AppEntity, AppListDelegate.ViewHolder>(), Filterable {

    var appList: MutableList<AppEntity> = mutableListOf()

    var isFiltered = false

    val db = Room.databaseBuilder(mContext, AppDatabase::class.java, "app").build()

    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.appIcon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.appPackage
        binding.isSelected.isChecked = item.isSelected
        binding.isOnly.isChecked = item.isOnly

        binding.isOnly.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                (adapterItems[holder.bindingAdapterPosition] as AppEntity).isOnly = isChecked
                db.appDao().updateApp(item)
            }
        }

        binding.isSelected.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                (adapterItems[holder.bindingAdapterPosition] as AppEntity).isSelected = isChecked
                db.appDao().updateApp(item)
            }
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
                    appList = adapterItems as MutableList<AppEntity>
                }
                if (constraint!!.isEmpty()) {
                    adapterItems = appList
                } else {
                    val tmp: MutableList<AppEntity> = mutableListOf()
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
                adapter.notifyDataSetChanged()
            }

        }
    }
}