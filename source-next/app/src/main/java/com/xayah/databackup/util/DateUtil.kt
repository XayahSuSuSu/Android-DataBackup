package com.xayah.databackup.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {
    fun timestampToDate(timestamp: Long): String =
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
}
