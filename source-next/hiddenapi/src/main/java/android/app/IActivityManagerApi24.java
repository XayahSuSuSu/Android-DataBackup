/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import dev.rikka.tools.refine.RefineAs;

/**
 * System private API for talking with the activity manager service.  This
 * provides calls from the application back to the activity manager.
 *
 * This declaration covers the API 24–25 method signatures.
 * {@hide}
 */
@RefineAs(IActivityManager.class)
public interface IActivityManagerApi24 extends IInterface {
    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token) throws RemoteException;

    /**
     * Information you can retrieve about a particular application.
     */
    class ContentProviderHolder {
        public IContentProvider provider;
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/app/IActivityManager.java
