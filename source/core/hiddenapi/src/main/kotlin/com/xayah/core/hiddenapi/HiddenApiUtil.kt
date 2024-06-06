package com.xayah.core.hiddenapi

inline fun <reified T> Any.castTo(): T {
    return this as T
}
