package com.xayah.core.util

import java.util.Locale

fun String.toPathList() = split("/")

/**
 * Return [ifSameValue] if [value] is equal to [this], else [value]
 */
fun String.ifNotTheSame(value: String, ifSameValue: String) = if (this == value) ifSameValue else value

fun String.capitalizeString() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.ROOT
    ) else it.toString()
}