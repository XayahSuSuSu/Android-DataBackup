package com.xayah.hiddenapi

inline fun <reified T> Any.castTo(): T {
    return this as T
}
