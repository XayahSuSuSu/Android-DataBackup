package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.databackup.util.ShellUtil


class AppListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<AppListAdapter.Holder>() {
    class Holder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)

    var appList: MutableList<AppInfo> = mutableListOf<AppInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterAppListBinding
                .inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val mShell = ShellUtil(mContext)
        val current = appList[position]
        val binding = holder.binding
        binding.adapterIcon.setImageDrawable(current.appIcon)
        binding.adapterName.text = current.appName
        binding.adapterPackage.text = current.appPackage
        binding.adapterBan.isChecked = !appList[position].ban
        binding.adapterOnlyApp.isChecked = appList[position].onlyApp

        binding.adapterBan.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mShell.allowAppByIndex(position)
            } else {
                mShell.banAppByIndex(position)
            }
        }
        binding.adapterOnlyApp.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                mShell.unLimitAppByIndex(position)
            } else {
                mShell.limitAppByIndex(position)
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
}