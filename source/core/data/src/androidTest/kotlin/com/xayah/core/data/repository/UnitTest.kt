package com.xayah.core.data.repository;

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topjohnwu.superuser.Shell
import com.xayah.core.util.command.PackageUtil
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnitTest {
    /**
     * @see <a href="https://github.com/MuntashirAkon/AppManager/issues/82#issuecomment-696296631">MuntashirAkon/AppManager#82</a>
     */
    @Test
    fun testKeyStoreDetection() {
        val uid = 10481
        Shell.cmd("su $uid -c keystore_cli_v2 list").exec().apply {
            println(out)
        }
        println(runBlocking { PackageUtil.hasKeystore("su", uid) })
    }
}
