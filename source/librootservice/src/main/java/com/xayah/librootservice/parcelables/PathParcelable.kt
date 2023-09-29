package com.xayah.librootservice.parcelables

import android.os.Parcel
import android.os.Parcelable

class PathParcelable() : Parcelable {
    var pathList: List<String> = listOf()
    var pathString: String = ""
    var nameWithoutExtension: String = ""
    var extension: String = ""

    constructor(pathList: List<String>, pathString: String, nameWithoutExtension: String, extension: String) : this() {
        this.pathList = pathList
        this.pathString = pathString
        this.nameWithoutExtension = nameWithoutExtension
        this.extension = extension
    }

    constructor(parcel: Parcel) : this() {
        mutableListOf<String>().also {
            parcel.readStringList(it)
            pathList = it
        }
        pathString = parcel.readString() ?: ""
        nameWithoutExtension = parcel.readString() ?: ""
        extension = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(pathList)
        parcel.writeString(pathString)
        parcel.writeString(nameWithoutExtension)
        parcel.writeString(extension)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PathParcelable> {
        override fun createFromParcel(parcel: Parcel): PathParcelable {
            return PathParcelable(parcel)
        }

        override fun newArray(size: Int): Array<PathParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
