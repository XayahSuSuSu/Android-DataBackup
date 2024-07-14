package com.xayah.core.model.util

import android.os.Build
import com.xayah.core.model.CompressionType
import com.xayah.core.model.KillAppOption
import com.xayah.core.model.LZ4_SUFFIX
import com.xayah.core.model.OpType
import com.xayah.core.model.SFTPAuthMode
import com.xayah.core.model.SelectionType
import com.xayah.core.model.SmbAuthMode
import com.xayah.core.model.SortType
import com.xayah.core.model.TAR_SUFFIX
import com.xayah.core.model.ThemeType
import com.xayah.core.model.ZSTD_SUFFIX
import java.text.DecimalFormat
import kotlin.math.pow

fun Double.formatSize(unitValue: Int = 1024): String = run {
    var unit = "Bytes"
    var size = this
    val gb = unitValue.toDouble().pow(3)
    val mb = unitValue.toDouble().pow(2)
    val kb = unitValue.toDouble()
    if (this > gb) {
        size = this / gb
        unit = "GB"
    } else if (this > mb) {
        size = this / mb
        unit = "MB"
    } else if (this > kb) {
        size = this / kb
        unit = "KB"
    }
    if (size == 0.0) "0.00 $unit" else "${DecimalFormat("#.00").format(size)} $unit"
}

fun CompressionType.Companion.of(name: String?): CompressionType =
    runCatching { CompressionType.valueOf(name!!.uppercase()) }.getOrDefault(CompressionType.ZSTD)

fun OpType.Companion.of(name: String?): OpType =
    runCatching { OpType.valueOf(name!!.uppercase()) }.getOrDefault(OpType.BACKUP)

fun SortType.Companion.of(name: String?): SortType =
    runCatching { SortType.valueOf(name!!.uppercase()) }.getOrDefault(SortType.ASCENDING)

fun CompressionType.Companion.suffixOf(suffix: String): CompressionType? = when (suffix) {
    TAR_SUFFIX -> CompressionType.TAR
    ZSTD_SUFFIX -> CompressionType.ZSTD
    LZ4_SUFFIX -> CompressionType.LZ4
    else -> null
}

fun SelectionType.Companion.of(name: String?): SelectionType =
    runCatching { SelectionType.valueOf(name!!.uppercase()) }.getOrDefault(SelectionType.DEFAULT)

fun ThemeType.Companion.of(name: String?): ThemeType =
    runCatching { ThemeType.valueOf(name!!.uppercase()) }.getOrDefault(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ThemeType.AUTO else ThemeType.LIGHT_THEME)

fun SmbAuthMode.Companion.indexOf(index: Int): SmbAuthMode = when (index) {
    1 -> SmbAuthMode.GUEST
    2 -> SmbAuthMode.ANONYMOUS
    else -> SmbAuthMode.PASSWORD
}

fun SFTPAuthMode.Companion.indexOf(index: Int): SFTPAuthMode = when (index) {
    1 -> SFTPAuthMode.PUBLIC_KEY
    else -> SFTPAuthMode.PASSWORD
}

fun KillAppOption.Companion.indexOf(index: Int): KillAppOption = when (index) {
    1 -> KillAppOption.OPTION_I
    2 -> KillAppOption.OPTION_II
    else -> KillAppOption.DISABLED
}

fun KillAppOption.Companion.of(name: String?): KillAppOption =
    runCatching { KillAppOption.valueOf(name!!.uppercase()) }.getOrDefault(KillAppOption.OPTION_II)
