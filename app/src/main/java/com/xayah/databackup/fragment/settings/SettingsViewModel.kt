package com.xayah.databackup.fragment.settings

import android.content.Context
import android.widget.CompoundButton
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.SettingsPreferencesDataStore
import com.xayah.databackup.view.preference.EditableText
import com.xayah.databackup.view.preference.SelectableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    var systemName = false

    var toastInfo = false

    var update = false

    var updateBehavior = false

    var lo = false

    var splist = false

    var backupUserData = false

    var backupObbData = false

    var backupMedia = false

    var usbDefault = false

    val compressionMethodItems: Array<String> = arrayOf("lz4", "zstd", "tar")

    var compressionMethod = ""

    var compressionMethodIndex = 0

    var info = ""

    var outputPath = ""

    var customPath = ""

     fun initialize(mContext: Context) {
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getSystemName(mContext).collect {
                 systemName = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getToastInfo(mContext).collect {
                 toastInfo = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getUpdate(mContext).collect {
                 update = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getUpdateBehavior(mContext).collect {
                 updateBehavior = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getLo(mContext).collect {
                 lo = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getSplist(mContext).collect {
                 splist = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getBackupUserData(mContext).collect {
                 backupUserData = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getBackupObbData(mContext).collect {
                 backupObbData = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getBackupMedia(mContext).collect {
                 backupMedia = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getUsbDefault(mContext).collect {
                 usbDefault = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getCompressionMethod(mContext).collect {
                 compressionMethod = it
                 compressionMethodIndex = compressionMethodItems.indexOf(compressionMethod)
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getInfo(mContext).collect {
                 info = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getOutputPath(mContext).collect {
                 outputPath = it
             }
         }
         CoroutineScope(Dispatchers.IO).launch {
             SettingsPreferencesDataStore.getCustomPath(mContext).collect {
                 customPath = it
             }
         }
     }

    val onSystemName: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveSystemName(v.context, isChecked)
        }
    }

    val onToastInfo: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveToastInfo(v.context, isChecked)
        }
    }

    val onUpdate: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveUpdate(v.context, isChecked)
        }
    }

    val onUpdateBehavior: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveUpdateBehavior(v.context, isChecked)
        }
    }

    val onLo: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveLo(v.context, isChecked)
        }
    }

    val onSplist: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveSplist(v.context, isChecked)
        }
    }

    val onBackupUserData: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                SettingsPreferencesDataStore.saveBackupUserData(v.context, isChecked)
            }
        }

    val onBackupObbData: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            CoroutineScope(Dispatchers.IO).launch {
                SettingsPreferencesDataStore.saveBackupObbData(v.context, isChecked)
            }
        }

    val onBackupMedia: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveBackupMedia(v.context, isChecked)
        }
    }

    val onUsbdefault: (buttonView: CompoundButton, isChecked: Boolean) -> Unit = { v, isChecked ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveUsbDefault(v.context, isChecked)
        }
    }

    val onCompressionMethod: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveCompressionMethod(
                v.context,
                compressionMethodItems[choice]
            )
        }
    }

    val onInfo: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveInfo(v.context, content.toString())
        }
    }

    val onOutputPath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveOutputPath(v.context, content.toString())
        }
    }

    val onCustomPath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        CoroutineScope(Dispatchers.IO).launch {
            SettingsPreferencesDataStore.saveCustomPath(v.context, content.toString())
        }
    }
}