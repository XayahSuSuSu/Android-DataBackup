package com.xayah.databackup.fragment.more

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import com.xayah.databackup.R

class MoreViewModel : ViewModel() {
    fun toSettingsFragment(v: View) {
        Navigation.findNavController(v).navigate(R.id.action_page_more_to_page_settings)
    }

    fun toAboutFragment(v: View) {
        Navigation.findNavController(v).navigate(R.id.action_page_more_to_page_about)
    }
}