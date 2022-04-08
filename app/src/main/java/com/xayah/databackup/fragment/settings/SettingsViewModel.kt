package com.xayah.databackup.fragment.settings

import android.content.Context
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import com.xayah.databackup.R
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.saveBackupSavePath
import com.xayah.databackup.util.saveCompressionType
import com.xayah.design.preference.EditableText
import com.xayah.design.preference.SelectableList

class SettingsViewModel : ViewModel() {
    val compressionTypeItems: Array<String> = arrayOf("tar", "zstd", "lz4")
    var compressionTypeIndex = 1

    var backupSavePath = ""

    fun initialize(context: Context) {
        backupSavePath = context.readBackupSavePath()

        compressionTypeIndex = compressionTypeItems.indexOf(context.readCompressionType())
    }

    val changeBackupSavePath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        v.context.saveBackupSavePath(content.toString().trim())
    }

    val changeCompressionType: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        v.context.saveCompressionType(compressionTypeItems[choice])
    }

    fun toAboutFragment(v: View) {
        Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_aboutFragment)
    }
}