package com.xayah.databackup.util

import java.text.DecimalFormat
import kotlin.math.pow

private const val UNIT = 1024F

const val DefStorageSize = "0.00 Bytes"

val Long.formatToStorageSize: String
    get() {
        var unit = "Bytes"
        var size = this.toFloat()
        val gb = UNIT.pow(3)
        val mb = UNIT.pow(2)
        val kb = UNIT
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
        return if (size == 0F) "0.00 $unit" else "${DecimalFormat("#.00").format(size)} $unit"
    }

val Long.formatToStorageSizePerSecond: String
    get() {
        var unit = "Bytes/S"
        var size = this.toFloat()
        val gb = UNIT.pow(3)
        val mb = UNIT.pow(2)
        val kb = UNIT
        if (this > gb) {
            size = this / gb
            unit = "GB/S"
        } else if (this > mb) {
            size = this / mb
            unit = "MB/S"
        } else if (this > kb) {
            size = this / kb
            unit = "KB/S"
        }
        return if (size == 0F) "0.00 $unit" else "${DecimalFormat("#.00").format(size)} $unit"
    }

