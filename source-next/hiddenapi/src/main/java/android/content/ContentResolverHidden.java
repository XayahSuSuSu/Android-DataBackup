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
 * This class provides applications access to the content model.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about using a ContentResolver with content providers, read the
 * <a href="{@docRoot}guide/topics/providers/content-providers.html">Content Providers</a>
 * developer guide.</p>
 */
@RefineAs(ContentResolver.class)
public abstract class ContentResolverHidden extends ContentResolver {
    public ContentResolverHidden(Context context) {
        super(context);
    }

    protected abstract IContentProvider acquireProvider(Context c, String name);

    /**
     * Providing a default implementation of this, to avoid having to change a
     * lot of other things, but implementations of ContentResolver should
     * implement it.
     *
     * @hide
     */
    protected abstract IContentProvider acquireExistingProvider(Context c, String name);

    protected abstract IContentProvider acquireUnstableProvider(Context c, String name);

    public abstract boolean releaseProvider(IContentProvider icp);

    public abstract boolean releaseUnstableProvider(IContentProvider icp);

    public abstract void unstableProviderDied(IContentProvider icp);
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/content/ContentResolver.java
