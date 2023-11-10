package com.xayah.core.util

fun String.toPathList() = split("/")

/**
 * Return [ifSameValue] if [value] is equal to [this], else [value]
 */
fun String.ifNotTheSame(value: String, ifSameValue: String) = if (this == value) ifSameValue else value