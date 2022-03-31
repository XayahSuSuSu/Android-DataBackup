package com.xayah.crash

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.xayah.crash.databinding.ActivityCrashBinding
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import java.io.File
import java.io.FileOutputStream

class CrashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val logs = intent.getStringExtra("crashInfo") ?: "\n"
        val that = this
        val materialYouFileExplorer = MaterialYouFileExplorer().apply { initialize(that) }
        binding.logs = logs
        binding.filledButton.setOnClickListener {
            materialYouFileExplorer.toExplorer(
                that, false, "default", arrayListOf(), true
            ) { path, _ ->
                try {
                    val date = logs.split("\n")[0]
                    val fileName = "Crash-${date.replace(" ", "-").replace(":", "-")}.txt"
                    val crashDir = File(path)
                    if (!crashDir.exists()) crashDir.mkdirs()
                    val fileOutputStream = FileOutputStream("${path}/$fileName", true)
                    fileOutputStream.write(logs.toByteArray())
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    Toast.makeText(that, getString(R.string.save_successful), Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(that, getString(R.string.save_failed), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}

