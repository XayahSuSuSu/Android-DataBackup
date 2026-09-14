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

package android.content;

import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

/**
 * A description of an Intent and target action to perform with it.
 * The returned object can be
 * handed to other applications so that they can perform the action you
 * described on your behalf at a later time.
 *
 * <p>By giving a IntentSender to another application,
 * you are granting it the right to perform the operation you have specified
 * as if the other application was yourself (with the same permissions and
 * identity).  As such, you should be careful about how you build the IntentSender:
 * often, for example, the base Intent you supply will have the component
 * name explicitly set to one of your own components, to ensure it is ultimately
 * sent there and nowhere else.
 *
 * <p>A IntentSender itself is simply a reference to a token maintained by
 * the system describing the original data used to retrieve it.  This means
 * that, even if its owning application's process is killed, the
 * IntentSender itself will remain usable from other processes that
 * have been given it.  If the creating application later re-retrieves the
 * same kind of IntentSender (same operation, same Intent action, data,
 * categories, and components, and same flags), it will receive a IntentSender
 * representing the same token if that is still valid.
 *
 * <p>Instances of this class can not be made directly, but rather must be
 * created from an existing {@link android.app.PendingIntent} with
 * {@link android.app.PendingIntent#getIntentSender() PendingIntent.getIntentSender()}.
 */
@RefineAs(IntentSender.class)
public class IntentSenderHidden {
    public IntentSenderHidden(IBinder target) {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/content/IntentSender.java
