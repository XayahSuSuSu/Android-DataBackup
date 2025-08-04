package com.xayah.databackup.util

import android.os.Parcel
import android.os.Parcelable

object ParcelableHelper {
    fun ByteArray.unmarshall(block: (parcel: Parcel) -> Unit) {
        val parcel = Parcel.obtain()
        parcel.unmarshall(this, 0, this.size)
        parcel.setDataPosition(0)
        block(parcel)
        parcel.recycle()
    }

    fun Parcelable.marshall(): ByteArray {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        this.writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        return bytes
    }
}
