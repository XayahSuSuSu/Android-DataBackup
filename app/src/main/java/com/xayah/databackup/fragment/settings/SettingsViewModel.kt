package com.xayah.databackup.fragment.settings

import android.view.View
import android.widget.CompoundButton
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.util.*
import com.xayah.design.preference.EditableText
import com.xayah.design.preference.SelectableList

class SettingsViewModel() : ViewModel() {
    val compressionTypeItems: Array<String> = arrayOf("tar", "zstd", "lz4")
    var compressionTypeIndex = compressionTypeItems.indexOf(App.globalContext.readCompressionType())
    val changeCompressionType: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        v.context.saveCompressionType(compressionTypeItems[choice])
    }

    var backupSavePath = App.globalContext.readBackupSavePath()
    val changeBackupSavePath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        v.context.saveBackupSavePath(content.toString().trim())
    }

    var isCustomDirectoryPath: ObservableField<Boolean> =
        ObservableField(App.globalContext.readIsCustomDirectoryPath())
    val onCustomDirectoryPath: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            v.context.saveIsCustomDirectoryPath(isChecked)
            isCustomDirectoryPath.set(isChecked)
        }
    var customDirectoryPath = App.globalContext.readCustomDirectoryPath()
    val changeCustomDirectoryPath: (v: EditableText, content: CharSequence?) -> Unit =
        { v, content ->
            v.context.saveCustomDirectoryPath(content.toString().trim())
        }

    var isDynamicColors: ObservableField<Boolean> =
        ObservableField(App.globalContext.readIsDynamicColors())
    val onDynamicColors: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            v.context.saveIsDynamicColors(isChecked)
        }

    fun toAboutFragment(v: View) {
        Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_aboutFragment)
    }
}