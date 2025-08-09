package com.xayah.dex;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableHelper {

    public interface ParcelBlock {
        void accept(Parcel parcel);
    }

    public static void unmarshall(byte[] byteArray, ParcelBlock block) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);
        block.accept(parcel);
        parcel.recycle();
    }

    public static byte[] marshall(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }
}
