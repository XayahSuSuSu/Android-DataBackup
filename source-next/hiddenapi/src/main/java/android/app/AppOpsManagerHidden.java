/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.os.Build;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppOpsManager.class)
public class AppOpsManagerHidden {
    public static final int OP_WRITE_SMS = 15;

    /**
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/app/AppOpsManager.java;l=494">AppOpsManager.java</a>
     */
    @RequiresApi(Build.VERSION_CODES.P)
    public static final String OPSTR_WRITE_SMS = "android:write_sms";

    /**
     * Like {@link #checkOp} but instead of throwing a {@link SecurityException} it
     * returns {@link #MODE_ERRORED}.
     *
     * @hide
     */
    public int checkOpNoThrow(int op, int uid, String packageName) {
        throw new RuntimeException("Stub!");
    }

    public void setMode(int code, int uid, String packageName, int mode) {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/app/AppOpsManager.java
