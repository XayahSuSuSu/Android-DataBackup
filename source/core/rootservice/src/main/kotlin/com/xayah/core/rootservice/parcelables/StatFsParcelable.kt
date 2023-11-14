package com.xayah.core.rootservice.parcelables

import android.os.Parcel
import android.os.Parcelable

class StatFsParcelable() : Parcelable {
    var availableBytes: Long = 0L
    var totalBytes: Long = 0L

    constructor(availableBytes: Long, totalBytes: Long) : this() {
        this.availableBytes = availableBytes
        this.totalBytes = totalBytes
    }

    constructor(parcel: Parcel) : this() {
        availableBytes = parcel.readLong()
        totalBytes = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(availableBytes)
        parcel.writeLong(totalBytes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StatFsParcelable> {
        override fun createFromParcel(parcel: Parcel): StatFsParcelable {
            return StatFsParcelable(parcel)
        }

        override fun newArray(size: Int): Array<StatFsParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
