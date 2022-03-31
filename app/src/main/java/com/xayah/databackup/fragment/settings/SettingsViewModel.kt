package com.xayah.databackup.fragment.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xayah.databackup.R
import com.xayah.databackup.util.readPreferences
import com.xayah.databackup.util.savePreferences
import com.xayah.design.preference.EditableText
import com.xayah.design.preference.SelectableList

class SettingsViewModel : ViewModel() {
    val compressionTypeItems: Array<String> = arrayOf("tar", "zstd", "lz4")
    var compressionTypeIndex = 2

    var backupSavePath = ""

    fun initialize(context: Context) {
        backupSavePath =
            context.readPreferences("backup_save_path")
                ?: context.getString(R.string.default_backup_save_path)

        compressionTypeIndex =
            compressionTypeItems.indexOf(context.readPreferences("compression_type") ?: "lz4")
    }

    val changeBackupSavePath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        v.context.savePreferences("backup_save_path", content.toString())
    }

    val changeCompressionType: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        v.context.savePreferences("compression_type", compressionTypeItems[choice])
    }
}