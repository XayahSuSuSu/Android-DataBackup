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
    source: OutputStream,
    interval: Duration = 1.seconds,
    onProgress: ((bytesWritten: Long, speed: Long) -> Unit)? = null
) : OutputStream() {
    private val mSource = source
    private val mInterval = interval
    private val mOnProgress = onProgress

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
        if (mOnProgress != null) {
            mListener = mScope.launch {
                while (isActive && mIsClosed.not()) {
                    val currentBytes = mBytesWritten
                    val delta = currentBytes - mLastBytesWritten
                    val speed = delta / mInterval.inWholeSeconds
                    mLastBytesWritten = currentBytes
                    if (mIsClosed.not() && currentBytes != 0L) {
                        mOnProgress(currentBytes, speed)
                    }
                    delay(mInterval)
                }
            }
        }
    }

    override fun write(b: Int) {
        mSource.write(b)
        mBytesWritten++
    }

    override fun write(b: ByteArray) {
        mSource.write(b)
        mBytesWritten += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        mSource.write(b, off, len)
        mBytesWritten += len
    }

    override fun flush() = mSource.flush()

    override fun close() {
        mIsClosed = true
        mEndTimestamp = System.currentTimeMillis()
        if (mBytesWritten != 0L) {
            val speed = (mBytesWritten / ((mEndTimestamp - mStartTimestamp).toFloat() / 1000)).roundToLong()
            mOnProgress?.invoke(mBytesWritten, speed)
        }
        mListener?.cancel()
        mSource.close()
    }
}
