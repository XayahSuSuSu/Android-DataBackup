/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.os.Build;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInstaller.class)
public class PackageInstallerHidden {
    /**
     * Android 8.0–11 constructor signature.
     * <p>
     * See <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r1:frameworks/base/core/java/android/content/pm/PackageInstaller.java">PackageInstaller.java</a>.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public PackageInstallerHidden(IPackageInstaller installer, String installerPackageName, int userId) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Android 12+ constructor signature.
     * <p>
     * See <a href="https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/core/java/android/content/pm/PackageInstaller.java">PackageInstaller.java</a>.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public PackageInstallerHidden(IPackageInstaller installer, String installerPackageName, String installerAttributionTag, int userId) {
        throw new RuntimeException("Stub!");
    }
}
