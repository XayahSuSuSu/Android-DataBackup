package com.xayah.databackup.database.entity

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.state.ToggleableState
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "apps", primaryKeys = ["packageName", "userId"])
data class App(
    var packageName: String,
    var userId: Int,
    @Embedded(prefix = "info_") var info: Info,
    @Embedded(prefix = "option_") var option: Option,
    @Embedded(prefix = "storage_") var storage: Storage,
) {
    val pkgUserKey: String
        get() = "$packageName-$userId"

    val selectedBytes: Long
        get() {
            var size = 0L
            if (option.apk) size += storage.apkBytes
            if (option.internalData) size += storage.internalDataBytes
            if (option.externalData) size += storage.externalDataBytes
            if (option.additionalData) size += storage.additionalDataBytes
            return size
        }

    val totalBytes: Long
        get() = storage.apkBytes + storage.internalDataBytes + storage.externalDataBytes + storage.additionalDataBytes

    val toggleableState: ToggleableState
        get() = if (option.apk && option.internalData && option.externalData && option.additionalData) {
            ToggleableState.On
        } else if (option.apk.not() && option.internalData.not() && option.externalData.not() && option.additionalData.not()) {
            ToggleableState.Off
        } else {
            ToggleableState.Indeterminate
        }

    val isSystemApp: Boolean
        get() = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0

    val isSelected: Boolean
        get() = option.apk || option.internalData || option.externalData || option.additionalData

    val isDataAllSelected: Boolean
        get() = option.internalData && option.externalData && option.additionalData
}

data class AppInfo(
    var packageName: String,
    var userId: Int,
    @Embedded(prefix = "info_") var info: Info,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        packageName = parcel.readString() ?: "",
        userId = parcel.readInt(),
        info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Info.CREATOR::class.java.classLoader, Info::class.java)
        } else {
            parcel.readParcelable(Info.CREATOR::class.java.classLoader)
        } ?: Info(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeInt(userId)
        dest.writeParcelable(info, flags)
    }

    companion object CREATOR : Parcelable.Creator<AppInfo> {
        override fun createFromParcel(parcel: Parcel): AppInfo {
            return AppInfo(parcel)
        }

        override fun newArray(size: Int): Array<AppInfo?> {
            return arrayOfNulls(size)
        }
    }
}

data class AppStorage(
    var packageName: String,
    var userId: Int,
    @Embedded(prefix = "storage_") var storage: Storage,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        packageName = parcel.readString() ?: "",
        userId = parcel.readInt(),
        storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Storage.CREATOR::class.java.classLoader, Storage::class.java)
        } else {
            parcel.readParcelable(Storage.CREATOR::class.java.classLoader)
        } ?: Storage(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeInt(userId)
        dest.writeParcelable(storage, flags)
    }

    companion object CREATOR : Parcelable.Creator<AppStorage> {
        override fun createFromParcel(parcel: Parcel): AppStorage {
            return AppStorage(parcel)
        }

        override fun newArray(size: Int): Array<AppStorage?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class Info(
    @ColumnInfo(defaultValue = "0") var uid: Int = 0,
    @ColumnInfo(defaultValue = "") var label: String = "",
    @ColumnInfo(defaultValue = "") var versionName: String = "",
    @ColumnInfo(defaultValue = "0") var versionCode: Long = 0,
    @ColumnInfo(defaultValue = "0") var flags: Int = 0,
    @ColumnInfo(defaultValue = "0") var firstInstallTime: Long = 0,
    @ColumnInfo(defaultValue = "0") var lastUpdateTime: Long = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        uid = parcel.readInt(),
        label = parcel.readString() ?: "",
        versionName = parcel.readString() ?: "",
        versionCode = parcel.readLong(),
        flags = parcel.readInt(),
        firstInstallTime = parcel.readLong(),
        lastUpdateTime = parcel.readLong()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(uid)
        dest.writeString(label)
        dest.writeString(versionName)
        dest.writeLong(versionCode)
        dest.writeInt(this.flags)
        dest.writeLong(firstInstallTime)
        dest.writeLong(lastUpdateTime)
    }

    companion object CREATOR : Parcelable.Creator<Info> {
        override fun createFromParcel(parcel: Parcel): Info {
            return Info(parcel)
        }

        override fun newArray(size: Int): Array<Info?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class Option(
    @ColumnInfo(defaultValue = "1") var apk: Boolean = true,
    @ColumnInfo(defaultValue = "1") var internalData: Boolean = true,
    @ColumnInfo(defaultValue = "1") var externalData: Boolean = true,
    @ColumnInfo(defaultValue = "1") var additionalData: Boolean = true,
)

@Serializable
data class Storage(
    @ColumnInfo(defaultValue = "0") var apkBytes: Long = 0,
    @ColumnInfo(defaultValue = "0") var internalDataBytes: Long = 0,
    @ColumnInfo(defaultValue = "0") var externalDataBytes: Long = 0,
    @ColumnInfo(defaultValue = "0") var additionalDataBytes: Long = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        apkBytes = parcel.readLong(),
        internalDataBytes = parcel.readLong(),
        externalDataBytes = parcel.readLong(),
        additionalDataBytes = parcel.readLong()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(apkBytes)
        dest.writeLong(internalDataBytes)
        dest.writeLong(externalDataBytes)
        dest.writeLong(additionalDataBytes)
    }

    companion object CREATOR : Parcelable.Creator<Storage> {
        override fun createFromParcel(parcel: Parcel): Storage {
            return Storage(parcel)
        }

        override fun newArray(size: Int): Array<Storage?> {
            return arrayOfNulls(size)
        }
    }
}
