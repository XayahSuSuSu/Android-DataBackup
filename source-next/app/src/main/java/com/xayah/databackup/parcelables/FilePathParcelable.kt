package com.xayah.databackup.parcelables

import android.os.Parcel
import android.os.Parcelable

class FilePathParcelable() : Parcelable {
    var path: String = ""
    var type: Int = 0 // 0: File, 1: Directory

    val isFile: Boolean get() = type == 0
    val isDirectory: Boolean get() = type == 1

    constructor(path: String, type: Int) : this() {
        this.path = path
        this.type = type
    }

    constructor(parcel: Parcel) : this() {
        path = parcel.readString().toString()
        type = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilePathParcelable> {
        override fun createFromParcel(parcel: Parcel): FilePathParcelable {
            return FilePathParcelable(parcel)
        }

        override fun newArray(size: Int): Array<FilePathParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
