package com.xayah.databackup.view.adapter

import androidx.databinding.BindingAdapter
import com.xayah.databackup.view.preference.SelectableList

class ViewAdapter {
    companion object {
        @BindingAdapter("items")
        @JvmStatic
        fun setItems(v: SelectableList, array: Array<String>?) {
            if (array != null) {
                v.setItems(array)
            }
        }
    }
}