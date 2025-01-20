/* //device/java/android/android/app/INotificationManager.aidl
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.app;

import android.app.NotificationChannel;
import android.content.pm.ParceledListSlice;

/** {@hide} */
interface INotificationManager {
    void createNotificationChannels(String pkg, in ParceledListSlice channelsList);
    void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id,
            in Notification notification, int userId);
}

// https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/app/INotificationManager.aidl