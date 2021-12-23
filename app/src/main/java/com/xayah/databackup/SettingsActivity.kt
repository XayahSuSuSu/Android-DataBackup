package com.xayah.databackup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.xayah.databackup.databinding.ActivitySettingsBinding
import com.xayah.databackup.preference.category
import com.xayah.databackup.preference.preferenceScreen
import com.xayah.databackup.preference.switch
import com.xayah.databackup.util.Shell
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var mShell: Shell
    lateinit var mContext: Context
    lateinit var binding: ActivitySettingsBinding
    lateinit var editor: SharedPreferences.Editor
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        setTitle(getString(R.string.title_Settings))
        mContext = this
        binding()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        mShell.saveSettings()
    }

    fun init() {
        mShell = Shell(this)
        editor = getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        GlobalScope.launch {
            runOnUiThread {
                val screen = preferenceScreen(mContext) {
                    category(R.string.settings_backup_setttings)
                    switch(
                        {
                            editor.putInt("Lo", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("Lo", 0) == 1
                        },
                        title = R.string.settings_title_lo,
                        summary = R.string.settings_summary_lo,
                    )
                    switch(
                        {
                            editor.putInt("Splist", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("Splist", 0) == 1
                        },
                        title = R.string.settings_title_splist,
                        summary = R.string.settings_summary_splist,
                    )
                    switch(
                        {
                            editor.putInt("Backup_user_data", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("Backup_user_data", 1) == 1
                        },
                        title = R.string.settings_title_backup_user_data,
                        summary = R.string.settings_summary_backup_user_data,
                    )
                    switch(
                        {
                            editor.putInt("Backup_obb_data", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("Backup_obb_data", 1) == 1
                        },
                        title = R.string.settings_title_backup_obb_data,
                        summary = R.string.settings_summary_backup_obb_data,
                    )
                    switch(
                        {
                            editor.putInt("backup_media", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("backup_media", 0) == 1
                        },
                        title = R.string.settings_title_backup_media,
                        summary = R.string.settings_summary_backup_media,
                    )
                    switch(
                        {
                            editor.putInt("USBdefault", if (it) 1 else 0).apply()
                        },
                        {
                            it.switchView.isChecked = prefs.getInt("USBdefault", 0) == 1
                        },
                        title = R.string.settings_title_usbdefault,
                        summary = R.string.settings_summary_usbdefault,
                    )
                    category(R.string.settings_path)
                    switch(
                        onShowSwitch = false,
                        title = R.string.settings_title_output_path,
                        summary = R.string.settings_sumarry_output_path,
                    )
                    switch(
                        onShowSwitch = false,
                        title = R.string.settings_title_custom_path,
                        summary = R.string.settings_summary_custom_path,
                    )
                }
                binding.content.addView(screen.root)
            }
        }

    }
}