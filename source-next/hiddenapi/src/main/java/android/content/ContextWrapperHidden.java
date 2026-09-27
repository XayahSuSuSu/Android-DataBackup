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

import dev.rikka.tools.refine.RefineAs;

/**
 * Proxying implementation of Context that simply delegates all of its calls to
 * another Context.  Can be subclassed to modify behavior without changing
 * the original Context.
 */
@RefineAs(ContextWrapper.class)
public class ContextWrapperHidden extends ContextWrapper {
    public ContextWrapperHidden(Context base) {
        super(base);
    }

    public String getOpPackageName() {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/content/ContextWrapper.java
