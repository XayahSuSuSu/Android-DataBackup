package com.xayah.core.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.xayah.feature.crash.CrashHandler

/**
 * Workaround for CrashHandler
 * @see <a href="https://github.com/firebase/firebase-android-sdk/issues/2005">https://github.com/firebase/firebase-android-sdk/issues/2005</a>
 */
class CrashProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        if (context != null) {
            CrashHandler(context!!).initialize()
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
