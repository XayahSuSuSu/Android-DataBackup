package com.xayah.core.util

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.encodeURL(charset: Charset = StandardCharsets.UTF_8): String = URLEncoder.encode(this, charset.toString())
fun String.decodeURL(charset: Charset = StandardCharsets.UTF_8): String = URLDecoder.decode(this, charset.toString())

val encodedURLWithSpace = " ".encodeURL()
fun String.ifEmptyEncodeURLWithSpace() = ifEmpty { " " }.encodeURL()
