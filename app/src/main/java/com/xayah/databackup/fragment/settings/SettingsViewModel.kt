package com.xayah.databackup.fragment.settings

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.CompoundButton
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.Issue
import com.xayah.databackup.data.Release
import com.xayah.databackup.util.*
import com.xayah.design.preference.EditableText
import com.xayah.design.preference.SelectableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

class SettingsViewModel() : ViewModel() {
    val compressionTypeItems: Array<String> = arrayOf("tar", "zstd", "lz4")
    var compressionTypeIndex = compressionTypeItems.indexOf(App.globalContext.readCompressionType())
    val changeCompressionType: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        v.context.saveCompressionType(compressionTypeItems[choice])
        compressionTypeIndex = choice
    }
    val compressionTypeHelp: ((v: View) -> Unit) = { v ->
        MaterialAlertDialogBuilder(v.context)
            .setTitle(v.context.getString(R.string.compression_type))
            .setMessage(v.context.getString(R.string.compression_type_help))
            .setPositiveButton(v.context.getString(R.string.confirm)) { _, _ -> }
            .setCancelable(true)
            .show()
    }

    var backupSavePath = App.globalContext.readBackupSavePath()
    val changeBackupSavePath: (v: EditableText, content: CharSequence?) -> Unit = { v, content ->
        v.context.saveBackupSavePath(content.toString().trim())
        backupSavePath = content.toString().trim()
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
            customDirectoryPath = content.toString().trim()
        }

    var isDynamicColors: ObservableField<Boolean> =
        ObservableField(App.globalContext.readIsDynamicColors())
    val onDynamicColors: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            v.context.saveIsDynamicColors(isChecked)
            isDynamicColors.set(isChecked)
        }

    var isBackupItself: ObservableField<Boolean> =
        ObservableField(App.globalContext.readIsBackupItself())
    val onIsBackupItself: (buttonView: CompoundButton, isChecked: Boolean) -> Unit =
        { v, isChecked ->
            v.context.saveIsBackupItself(isChecked)
            isBackupItself.set(isChecked)
        }

    val userItems: Array<String> =
        if (Bashrc.listUsers().first) Bashrc.listUsers().second.toTypedArray() else arrayOf("0")
    var userIndex = userItems.indexOf(App.globalContext.readUser())
    val changeUser: ((v: SelectableList, choice: Int) -> Unit) = { v, choice ->
        v.context.saveUser(userItems[choice])
        userIndex = choice
    }
    val userHelp: ((v: View) -> Unit) = { v ->
        MaterialAlertDialogBuilder(v.context)
            .setTitle(v.context.getString(R.string.user) + " " + v.context.getString(R.string.beta_risk))
            .setMessage(v.context.getString(R.string.user_help))
            .setPositiveButton(v.context.getString(R.string.confirm)) { _, _ -> }
            .setCancelable(true)
            .show()
    }

    fun toAboutFragment(v: View) {
        Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_aboutFragment)
    }

    fun toHistoryDialog(v: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request: Request = Request.Builder()
                    .url("https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/releases")
                    .build()
                client.newCall(request).execute().use { response ->
                    response.body?.apply {
                        // 解析response.body
                        val jsonArray = JsonParser.parseString(this.string()).asJsonArray
                        val nameList = mutableListOf<String>()
                        val mBodyList = mutableListOf<Release>()
                        for (i in jsonArray) {
                            val item = Gson().fromJson(i, Release::class.java)
                            mBodyList.add(item)
                            nameList.add(item.name)
                        }
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(v.context)
                                .setTitle(v.context.getString(R.string.history_version))
                                .setItems(nameList.toTypedArray()) { _, index ->
                                    MaterialAlertDialogBuilder(v.context)
                                        .setTitle(nameList[index])
                                        .setMessage(mBodyList[index].body)
                                        .setPositiveButton(v.context.getString(R.string.github)) { _, _ ->
                                            toWebView(v, mBodyList[index].html_url)
                                        }
                                        .setCancelable(true)
                                        .show()
                                }
                                .setPositiveButton(v.context.getString(R.string.confirm)) { _, _ -> }
                                .setCancelable(true)
                                .show()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun toFeatureDialog(v: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request: Request = Request.Builder()
                    .url("https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/issues?state=open")
                    .build()
                client.newCall(request).execute().use { response ->
                    response.body?.apply {
                        // 解析response.body
                        val jsonArray = JsonParser.parseString(this.string()).asJsonArray
                        val titleList = mutableListOf<String>()
                        val mBodyList = mutableListOf<Issue>()
                        for (i in jsonArray) {
                            val item = Gson().fromJson(i, Issue::class.java)
                            mBodyList.add(item)
                            titleList.add(item.title)
                        }
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(v.context)
                                .setTitle(v.context.getString(R.string.feature_foresight))
                                .setItems(titleList.toTypedArray()) { _, index ->
                                    MaterialAlertDialogBuilder(v.context)
                                        .setTitle(titleList[index])
                                        .setMessage(mBodyList[index].body)
                                        .setPositiveButton(v.context.getString(R.string.github)) { _, _ ->
                                            toWebView(v, mBodyList[index].html_url)
                                        }
                                        .setCancelable(true)
                                        .show()
                                }
                                .setPositiveButton(v.context.getString(R.string.confirm)) { _, _ -> }
                                .setCancelable(true)
                                .show()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun toWebView(v: View, url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        v.context.startActivity(intent)
    }
}