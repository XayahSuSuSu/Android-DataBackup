package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewDelegate
import com.google.android.material.chip.Chip
import com.xayah.databackup.R
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding

open class AppListHeaderAdapterBase(
    val onInitialize: (binding: AdapterAppListHeaderBinding) -> Unit,
    val onChipAppClick: (v: Chip) -> Unit,
    val onChipDataClick: (v: Chip) -> Unit,
    val onSearchViewQueryTextChange: (newText: String?) -> Unit,
    val onNoneBtnClick: (v: Button) -> Unit,
    val onSelectedBtnClick: (v: Button) -> Unit,
    val onNotSelectedBtnClick: (v: Button) -> Unit,
    val showAppLoadType: Boolean = false,
    val onInstalledAppBtnClick: (v: Button) -> Unit = {},
    val onSystemAppBtnClick: (v: Button) -> Unit = {},
    val onAllAppBtnClick: (v: Button) -> Unit = {},
    val onDefAppLoadType: () -> Int = { 0 },
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
        binding.materialSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onSearchViewQueryTextChange(newText)
                return true
            }
        })
        binding.buttonNone.setOnClickListener {
            onNoneBtnClick(binding.buttonNone)
        }
        binding.buttonSelected.setOnClickListener {
            onSelectedBtnClick(binding.buttonSelected)
        }
        binding.buttonNotSelected.setOnClickListener {
            onNotSelectedBtnClick(binding.buttonNotSelected)
        }
        binding.materialButtonToggleGroupAppLoadType.visibility =
            if (showAppLoadType) View.VISIBLE else View.GONE
        binding.buttonInstalled.setOnClickListener {
            onInstalledAppBtnClick(binding.buttonInstalled)
            binding.materialButtonToggleGroupSelection.check(R.id.button_none)
        }
        binding.buttonSystem.setOnClickListener {
            onSystemAppBtnClick(binding.buttonSystem)
            binding.materialButtonToggleGroupSelection.check(R.id.button_none)
        }
        binding.buttonAll.setOnClickListener {
            onAllAppBtnClick(binding.buttonAll)
            binding.materialButtonToggleGroupSelection.check(R.id.button_none)
        }
        when (onDefAppLoadType()) {
            0 -> {
                // 安装应用
                binding.materialButtonToggleGroupAppLoadType.check(R.id.button_installed)
            }
            1 -> {
                // 系统应用
                binding.materialButtonToggleGroupAppLoadType.check(R.id.button_system)
            }
            2 -> {
                // 全部应用
                binding.materialButtonToggleGroupAppLoadType.check(R.id.button_all)
            }
        }
    }

    class ViewHolder(val binding: AdapterAppListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)
}