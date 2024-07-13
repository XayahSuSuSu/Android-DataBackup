package com.xayah.core.rootservice.parcelables

import android.os.Parcel
import android.os.Parcelable

class StorageStatsParcelable() : Parcelable {
    var appBytes: Long = 0L
    var cacheBytes: Long = 0L
    var dataBytes: Long = 0L
    var externalCacheBytes: Long = 0L

    constructor(appBytes: Long, cacheBytes: Long, dateBytes: Long, externalCacheBytes: Long): this() {
        this.appBytes = appBytes
        this.cacheBytes = cacheBytes
        this.dataBytes = dateBytes
        this.externalCacheBytes = externalCacheBytes
    }

    constructor(parcel: Parcel) : this() {
        appBytes = parcel.readLong()
        cacheBytes = parcel.readLong()
        dataBytes = parcel.readLong()
        externalCacheBytes = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(appBytes)
        parcel.writeLong(cacheBytes)
        parcel.writeLong(dataBytes)
        parcel.writeLong(externalCacheBytes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StorageStatsParcelable> {
        override fun createFromParcel(parcel: Parcel): StorageStatsParcelable {
            return StorageStatsParcelable(parcel)
        }

        override fun newArray(size: Int): Array<StorageStatsParcelable?> {
            return arrayOfNulls(size)
        }
    }

}