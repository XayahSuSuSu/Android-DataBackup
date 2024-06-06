package android.os;

import java.io.FileDescriptor;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/os/MemoryFile.java">MemoryFile.java</a>
 */
@RefineAs(MemoryFile.class)
public class MemoryFileHidden {
    public FileDescriptor getFileDescriptor() {
        throw new RuntimeException("Stub!");
    }
}
