package com.xayah.core.hfx

import org.junit.Assert.assertTrue
import org.junit.Test

class HFXServerEngineTest {
    @Test
    fun discoverInterfacesAlwaysContainsUsbAdb() {
        val list = NetInterfaceDiscoverer.discoverAll()
        assertTrue(list.any { it.name == NetInterfaceDiscoverer.USB_ADB_NAME && it.ipAddress == "127.0.0.1" })
    }

    @Test
    fun resolveEnabledFallsBackToUsbAdbWhenOffline() {
        val resolved = NetInterfaceDiscoverer.resolveEnabled(listOf("nonexistent_wlan99"), usbParallelChannels = 2)
        assertTrue(resolved.size == 2 && resolved[0].name == "USB_ADB_1" && resolved[1].name == "USB_ADB_2")
    }
}

