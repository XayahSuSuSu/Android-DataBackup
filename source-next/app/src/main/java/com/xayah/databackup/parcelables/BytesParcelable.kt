package com.xayah.databackup.parcelables

import android.os.Parcel
import android.os.Parcelable

class BytesParcelable() : Parcelable {
    var bytesSize: Int = 0
    var bytes: ByteArray = ByteArray(0)

    constructor(bytesSize: Int, bytes: ByteArray) : this() {
        this.bytesSize = bytesSize
        this.bytes = bytes
    }

    constructor(parcel: Parcel) : this() {
        bytesSize = parcel.readInt()
        val byteArray = ByteArray(bytesSize)
        parcel.readByteArray(byteArray)
        bytes = byteArray
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(bytesSize)
        parcel.writeByteArray(bytes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BytesParcelable> {
        override fun createFromParcel(parcel: Parcel): BytesParcelable {
            return BytesParcelable(parcel)
        }

        override fun newArray(size: Int): Array<BytesParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
