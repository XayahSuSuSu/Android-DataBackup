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

package android.content.pm;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * Class for retrieving various kinds of information related to the application
 * packages that are currently installed on the device.
 * <p>
 * You can find this class through {@link Context#getPackageManager}.
 */
@RefineAs(PackageManager.class)
public class PackageManagerHidden {
    /**
     * Flag parameter for {@link #installPackage} to indicate that you want to
     * replace an already installed package, if one exists.
     *
     * @hide
     */
    public static final int INSTALL_REPLACE_EXISTING = 0x00000002;

    /**
     * Flag parameter for {@link #installPackage} to indicate that you want to
     * allow test packages (those that have set android:testOnly in their
     * manifest) to be installed.
     *
     * @hide
     */
    public static final int INSTALL_ALLOW_TEST = 0x00000004;

    /**
     * Flag parameter for {@link #installPackage} to bypass the low targer sdk version block
     * for this install.
     *
     * @hide See <a href="https://cs.android.com/android/platform/superproject/+/android-14.0.0_r75:frameworks/base/core/java/android/content/pm/PackageManager.java;l=1777">PackageManager.java</a>
     */
    public static final int INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK = 0x01000000;

    public List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        throw new RuntimeException("Stub!");
    }

    public PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/content/pm/PackageManager.java
