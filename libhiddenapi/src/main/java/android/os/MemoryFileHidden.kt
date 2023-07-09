package android.os

import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.FileDescriptor

class MemoryFileHidden {
    companion object {
        fun getFileDescriptor(memoryFile: MemoryFile): FileDescriptor {
            return HiddenApiBypass.invoke(MemoryFile::class.java, memoryFile, "getFileDescriptor") as FileDescriptor
        }
    }
}
