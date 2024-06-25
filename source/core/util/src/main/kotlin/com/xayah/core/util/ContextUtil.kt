package com.xayah.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.toBrowser(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
