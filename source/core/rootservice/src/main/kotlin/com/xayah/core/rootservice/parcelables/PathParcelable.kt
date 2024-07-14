package com.xayah.core.rootservice.parcelables

import android.os.Parcel
import android.os.Parcelable
import java.nio.file.Paths
import kotlin.io.path.pathString

class PathParcelable() : Parcelable {
    var pathList: List<String> = listOf()
    var pathString: String = ""
    var nameWithoutExtension: String = ""
    var extension: String = ""

    constructor(pathString: String) : this() {
        this.pathList = pathString.split("/")
        this.pathString = pathString
        this.nameWithoutExtension = pathList.last().split(".").first()
        this.extension = pathList.last().replace("${nameWithoutExtension}.", "")
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
