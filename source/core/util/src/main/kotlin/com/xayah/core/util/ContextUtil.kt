package com.xayah.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.net.Uri

fun Context.toBrowser(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
fun Context.getBaseContext(): Context = (this as ContextResWrapper).baseContext
fun Context.getActivity(): Activity = this as Activity

class ContextResWrapper(private val resources: Context, base: Context) : ContextWrapper(base) {
    override fun getResources(): Resources {
        return resources.resources
    }
}
