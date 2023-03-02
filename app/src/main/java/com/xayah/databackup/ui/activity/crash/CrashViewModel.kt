package com.xayah.databackup.ui.activity.crash

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.util.GsonUtil
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CrashViewModel : ViewModel() {
    lateinit var explorer: MaterialYouFileExplorer
    var crashInfo = ""

    fun initializeExplorer(activity: ComponentActivity) {
        explorer = MaterialYouFileExplorer().apply {
            initialize(activity)
        }
    }

    fun saveCrashInfo(context: Context) {
        explorer.apply {
            isFile = false
            toExplorer(context) { path, _ ->
                viewModelScope.launch {
                    try {
                        val date = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.CHINA
                        ).format(Date())
                        val name =
                            "Crash-${date.replace(" ", "-").replace(":", "-")}.txt"
                        GsonUtil.saveToFile("${path}/${name}", crashInfo)
                        Toast.makeText(
                            context,
                            context.getString(R.string.success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
