/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import android.content.IContentProvider;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

/**
 * Information you can retrieve about a particular application.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.O)
public class ContentProviderHolder implements Parcelable {
    public IContentProvider provider;
    public static final Parcelable.Creator<ContentProviderHolder> CREATOR = null;

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/app/ContentProviderHolder.java
