package com.xayah.core.hfx

import top.weixiansen574.hybridfilexfer.core.bean.ServerNetInterface
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetInterfaceDiscoverer {
    const val USB_ADB_NAME = "USB_ADB"
    private val IGNORED_PREFIXES = listOf("rmnet", "ccmni", "pdp", "wwan", "dummy", "lo")

    data class DiscoveredInterface(
        val name: String,
        val ipAddress: String,
        val defaultEnabled: Boolean,
    )

    fun discoverAll(): List<DiscoveredInterface> {
        val result = mutableListOf<DiscoveredInterface>()
        result.add(DiscoveredInterface(USB_ADB_NAME, "127.0.0.1", true))

        val enumIfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return result
        val seenNames = mutableSetOf(USB_ADB_NAME)

        while (enumIfaces.hasMoreElements()) {
            val ni = enumIfaces.nextElement() ?: continue
            val isUp = runCatching { ni.isUp }.getOrDefault(false)
            if (!isUp) continue
            val name = ni.displayName ?: ni.name ?: continue
            val lower = name.lowercase()
            if (IGNORED_PREFIXES.any { lower.startsWith(it) }) continue
            if (seenNames.contains(name)) continue

            val addrs = ni.inetAddresses ?: continue
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val hostAddr = addr.hostAddress ?: continue
                    seenNames.add(name)
                    result.add(
                        DiscoveredInterface(
                            name = name,
                            ipAddress = hostAddr,
                            defaultEnabled = !lower.startsWith("tun"),
                        )
                    )
                    break
                }
            }
        }
        return result
    }

    fun resolveEnabled(enabledNames: List<String>, usbParallelChannels: Int = 2): List<ServerNetInterface> {
        val discovered = discoverAll().associateBy { it.name }
        val resolved = mutableListOf<ServerNetInterface>()
        val usbCount = usbParallelChannels.coerceIn(1, 24)
        for (name in enabledNames) {
            if (name == USB_ADB_NAME) {
                val lo = InetAddress.getByName("127.0.0.1")
                if (usbCount == 1) {
                    resolved.add(ServerNetInterface(USB_ADB_NAME, lo, null))
                } else {
                    for (idx in 1..usbCount) {
                        resolved.add(ServerNetInterface("${USB_ADB_NAME}_$idx", lo, null))
                    }
                }
                continue
            }
            val item = discovered[name] ?: continue
            resolved.add(ServerNetInterface(item.name, InetAddress.getByName(item.ipAddress), null))
        }
        if (resolved.isEmpty()) {
            val lo = InetAddress.getByName("127.0.0.1")
            for (idx in 1..usbCount) {
                val n = if (usbCount == 1) USB_ADB_NAME else "${USB_ADB_NAME}_$idx"
                resolved.add(ServerNetInterface(n, lo, null))
            }
        }
        return resolved
    }
}
