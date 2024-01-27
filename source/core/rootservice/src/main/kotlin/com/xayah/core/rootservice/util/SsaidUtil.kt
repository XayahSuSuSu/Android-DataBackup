// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later

package com.xayah.core.rootservice.util

import android.os.Build
import android.os.HandlerThread
import android.os.Process
import com.android.providers.settings.SettingsState
import com.android.providers.settings.SettingsStateApi26
import com.android.providers.settings.SettingsStateApi31
import com.xayah.core.util.PathUtil
import java.io.File

/**
 * @author <a href="https://github.com/MuntashirAkon">@MuntashirAkon</a>
 */
class SsaidUtil(userId: Int) {
    private val ssaidUserKey = "userkey"
    private val lock = Any()
    private var settingsState: SettingsState = HandlerThread("ssaid_handler", Process.THREAD_PRIORITY_BACKGROUND).let {
        it.start()
        val file = File(PathUtil.getSsaidPath(userId))
        val key = SettingsState.makeKey(SettingsState.SETTINGS_TYPE_SSAID, userId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsStateApi31(
                lock,
                file,
                key,
                SettingsState.MAX_BYTES_PER_APP_PACKAGE_UNLIMITED,
                it.looper
            )
        } else {
            SettingsStateApi26(
                lock,
                file,
                key,
                SettingsState.MAX_BYTES_PER_APP_PACKAGE_UNLIMITED,
                it.looper
            )
        }
    }

    fun getSsaid(packageName: String, uid: Int): String? = settingsState.getSettingLocked(getName(packageName, uid)).value
    fun setSsaid(packageName: String, uid: Int, ssaid: String) = settingsState.insertSettingLocked(getName(packageName, uid), ssaid, null, true, packageName);

    private fun getName(packageName: String?, uid: Int): String {
        return if (packageName == SettingsState.SYSTEM_PACKAGE_NAME) ssaidUserKey else uid.toString()
    }
}
