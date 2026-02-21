package com.xayah.databackup.data

const val STATUS_SUCCESS = 0
const val STATUS_ERROR = -1
const val STATUS_SKIP = -100
const val STATUS_CANCEL = -101

fun isFailedStatus(status: Int): Boolean {
    return status != STATUS_SUCCESS &&
        status != STATUS_SKIP &&
        status != STATUS_CANCEL
}
