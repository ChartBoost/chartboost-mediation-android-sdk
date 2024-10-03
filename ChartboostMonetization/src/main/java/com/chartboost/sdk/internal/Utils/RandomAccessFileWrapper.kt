package com.chartboost.sdk.internal.utils

import java.io.FileDescriptor
import java.io.RandomAccessFile

/**
 * Simple wrapper for RandomAccessFile to be able to mock in tests.
 * Native methods are not mockable (e.g. length()).
 */
internal class RandomAccessFileWrapper(private val randomAccessFile: RandomAccessFile) {
    val fd: FileDescriptor = randomAccessFile.fd

    fun length(): Long = randomAccessFile.length()

    fun close() {
        randomAccessFile.close()
    }
}
