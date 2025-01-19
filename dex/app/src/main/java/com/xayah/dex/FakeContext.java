package com.xayah.dex;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.os.Binder;

public final class FakeContext extends ContextWrapper {
    public FakeContext(Context base) {
        super(base);
    }

    private final ContentResolver contentResolver = new ContentResolver(this) {
        @SuppressWarnings({"unused", "ProtectedMemberInFinalClass"})
        // @Override (but super-class method not visible)
        protected IContentProvider acquireProvider(Context c, String name) {
            return HiddenApiHelper.getContentProviderExternal(name, new Binder());
        }

        @SuppressWarnings("unused")
        // @Override (but super-class method not visible)
        public boolean releaseProvider(IContentProvider icp) {
            return false;
        }

        @SuppressWarnings({"unused", "ProtectedMemberInFinalClass"})
        // @Override (but super-class method not visible)
        protected IContentProvider acquireUnstableProvider(Context c, String name) {
            return null;
        }

        @SuppressWarnings("unused")
        // @Override (but super-class method not visible)
        public boolean releaseUnstableProvider(IContentProvider icp) {
            return false;
        }

        @SuppressWarnings("unused")
        // @Override (but super-class method not visible)
        public void unstableProviderDied(IContentProvider icp) {
            // ignore
        }
    };

    @Override
    public ContentResolver getContentResolver() {
        return contentResolver;
    }
}

// https://github.com/Genymobile/scrcpy/blob/91373d906b100349de959f49172d4605f66f64b2/server/src/main/java/com/genymobile/scrcpy/FakeContext.java