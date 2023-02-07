package com.xayah.databackup.compose.ui.activity.crash

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.crash.components.CrashScaffold
import com.xayah.databackup.compose.ui.theme.DataBackupTheme
import com.xayah.databackup.util.GsonUtil
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalMaterial3Api
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val materialYouFileExplorer = MaterialYouFileExplorer().apply {
            initialize(this@CrashActivity)
        }

        val logs = intent.getStringExtra("crashInfo") ?: "\n"
        setContent {
            DataBackupTheme {
                CrashScaffold(logs) {
                    materialYouFileExplorer.apply {
                        isFile = false
                        toExplorer(this@CrashActivity) { path, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val date = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.CHINA
                                    ).format(Date())
                                    val name =
                                        "Crash-${date.replace(" ", "-").replace(":", "-")}.txt"
                                    GsonUtil.saveToFile(
                                        "${path}/${name}",
                                        logs
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@CrashActivity,
                                            getString(R.string.success),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@CrashActivity,
                                            getString(R.string.failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

