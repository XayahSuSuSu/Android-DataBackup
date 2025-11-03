package com.xayah.databackup.rootservice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CountingOutputStream(
    private val source: OutputStream,
    private val interval: Duration = 1.seconds,
    private val onProgress: ((bytesWritten: Long, speed: Long) -> Unit)? = null
) : OutputStream() {
    @Volatile
    private var mBytesWritten: Long = 0L
    private var mLastBytesWritten: Long = 0L
    private val mScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mListener: Job? = null
    private var mIsClosed: Boolean = false
    private var mStartTimestamp: Long = 0L
    private var mEndTimestamp: Long = 0L

    init {
        mStartTimestamp = System.currentTimeMillis()
        onListening()
    }

    private fun onListening() {
        if (onProgress != null) {
            mListener = mScope.launch {
                while (isActive && mIsClosed.not()) {
                    val currentBytes = mBytesWritten
                    val delta = currentBytes - mLastBytesWritten
                    val speed = delta / interval.inWholeSeconds
                    mLastBytesWritten = currentBytes
                    if (mIsClosed.not() && currentBytes != 0L) {
                        onProgress(currentBytes, speed)
                    }
                    delay(interval)
                }
            }
        }
    }

    override fun write(b: Int) {
        source.write(b)
        mBytesWritten++
    }

    override fun write(b: ByteArray) {
        source.write(b)
        mBytesWritten += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        source.write(b, off, len)
        mBytesWritten += len
    }

    override fun flush() = source.flush()

    override fun close() {
        mIsClosed = true
        mEndTimestamp = System.currentTimeMillis()
        if (mBytesWritten != 0L) {
            val speed = (mBytesWritten / ((mEndTimestamp - mStartTimestamp).toFloat() / 1000)).roundToLong()
            onProgress?.invoke(mBytesWritten, speed)
        }
        mListener?.cancel()
        source.close()
    }
}
