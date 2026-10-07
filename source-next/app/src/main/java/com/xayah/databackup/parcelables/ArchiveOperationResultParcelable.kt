package com.xayah.databackup.parcelables

import android.os.Parcel
import android.os.Parcelable

class ArchiveOperationResultParcelable(val mExitCode: Int, val mDiagnostics: String) : Parcelable {
    private constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readString().orEmpty())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(mExitCode)
        parcel.writeString(mDiagnostics)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ArchiveOperationResultParcelable> {
        override fun createFromParcel(parcel: Parcel): ArchiveOperationResultParcelable = ArchiveOperationResultParcelable(parcel)
        override fun newArray(size: Int): Array<ArchiveOperationResultParcelable?> = arrayOfNulls(size)
    }
}
