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

/**
 * This manages the execution of the main thread in an
 * application process, scheduling and executing activities,
 * broadcasts, and other operations on it as the activity
 * manager requests.
 * <p>
 * {@hide}
 */
public class ActivityThread {
    public static ActivityThread systemMain() {
        throw new RuntimeException("Stub!");
    }

    public ContextImpl getSystemContext() {
        throw new RuntimeException("Stub!");
    }
}


// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/app/ActivityThread.java