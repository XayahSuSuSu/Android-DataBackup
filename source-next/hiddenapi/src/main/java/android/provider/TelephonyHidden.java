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

package android.provider;

import android.os.Build;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Telephony.class)
public final class TelephonyHidden {
    public static final class ReadRestriction {
        /**
         * Specifies if the message is restricted.
         * <p>
         * This column can be set to {@code true} only when a new sms or pdu message is inserted and
         * the writer has the {@link AppOpsManager#OP_WRITE_RESTRICTED_MESSAGES} app op. Otherwise
         * it is set to {@code false}.
         * <p>
         * A restricted message is only visible to the apps with a
         * {@link AppOpsManager#OP_READ_RESTRICTED_MESSAGES} app op.
         * <p>
         * This column is present in the following views: "sms_all", "sms_restricted", "pdu_all" and
         * "pdu_restricted".
         *
         * <p>Type: BOOLEAN</p>
         *
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        public static final String RESTRICTED = "restricted";
    }
}

// https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/base/core/java/android/provider/Telephony.java
