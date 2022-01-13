package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.model.app.AppEntity
import com.xayah.databackup.util.DataUtil

class AppListDelegate(val mContext: Context) :
    ItemViewDelegate<AppEntity, AppListDelegate.ViewHolder>() {

    var appList: MutableList<AppEntity> = mutableListOf()

    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        val binding = holder.binding
        val (appIcon, appName, appPackage) = DataUtil.getAppInfo(mContext, item.appPackage)
        binding.appIcon.setImageDrawable(appIcon)
        binding.appName.text = appName
        binding.appPackage.text = appPackage
        binding.isSelected.isChecked = item.isSelected
        binding.isOnly.isChecked = item.isOnly
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }
}