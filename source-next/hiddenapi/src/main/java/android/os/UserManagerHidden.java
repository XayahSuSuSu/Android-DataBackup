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

package android.os;

import android.content.Context;
import android.content.pm.UserInfo;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * Manages users and user details on a multi-user system. There are two major categories of
 * users: fully customizable users with their own login, and managed profiles that share a workspace
 * with a related user.
 * <p>
 * Users are different from accounts, which are managed by
 * {@link AccountManager}. Each user can have their own set of accounts.
 * <p>
 * See {@link DevicePolicyManager#ACTION_PROVISION_MANAGED_PROFILE} for more on managed profiles.
 */
@RefineAs(UserManager.class)
public class UserManagerHidden {
    public static UserManager get(Context context) {
        throw new RuntimeException("Stub!");
    }

    public List<UserInfo> getUsers() {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/os/UserManager.java