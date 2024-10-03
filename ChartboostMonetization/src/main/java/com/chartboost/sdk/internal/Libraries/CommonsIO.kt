package com.chartboost.sdk.internal.Libraries

import java.io.*

/**
 * This utility class is based on the Apache Commons IO library
 * (see [Apache Commons IO](https://github.com/apache/commons-io))
 * to provide essential I/O operations without requiring the inclusion of the entire library.
 * These methods facilitate operations such as reading files into byte arrays,
 * copying streams, and handling I/O streams with improved error messages.
 */
object CommonsIO {
    /**
     * An empty byte array constant to use when size is zero.
     */
    private val EMPTY_BYTE_ARRAY = ByteArray(0)

    /**
     * The default buffer size to use in copy methods.
     */
    const val DEFAULT_BUFFER_SIZE = 8192

    /**
     * Represents the end-of-file (or stream).
     */
    const val EOF = -1

    /**
     * Opens a [FileInputStream] for the specified file, providing better
     * error messages than simply calling [FileInputStream].
     *
     * @param file the file to open for input, must not be null
     * @return a new [FileInputStream] for the specified file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be read
     */
    @Throws(IOException::class)
    fun openInputStream(file: File): FileInputStream {
        if (!file.exists()) {
            throw FileNotFoundException("File '${file.absolutePath}' does not exist")
        }
        if (file.isDirectory) {
            throw IOException("File '${file.absolutePath}' exists but is a directory")
        }
        if (!file.canRead()) {
            throw IOException("File '${file.absolutePath}' cannot be read")
        }
        return FileInputStream(file)
    }

    /**
     * Reads the contents of a file into a byte array.
     * The file is always closed.
     *
     * @param file the file to read, must not be null
     * @return the file contents, never null
     * @throws IOException in case of an I/O error
     */
    @Throws(IOException::class)
    fun readFileToByteArray(file: File): ByteArray {
        openInputStream(file).use { input ->
            val fileLength = file.length()
            return if (fileLength > 0) toByteArray(input, fileLength) else toByteArray(input)
        }
    }

    /**
     * Gets the contents of an [InputStream] as a [ByteArray].
     * This method buffers the input internally, so there is no need to use a [BufferedInputStream].
     *
     * @param input the [InputStream] to read from
     * @return the requested byte array
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun toByteArray(input: InputStream): ByteArray {
        ByteArrayOutputStream().use { output ->
            copy(input, output)
            return output.toByteArray()
        }
    }

    /**
     * Gets contents of an [InputStream] as a [ByteArray].
     * Use this method instead of [toByteArray] when [InputStream] size is known.
     *
     * @param input the [InputStream] to read from
     * @param size  the size of [InputStream]
     * @return the requested byte array
     * @throws IOException if an I/O error occurs or [InputStream] size differ from parameter size
     * @throws IllegalArgumentException if size is less than zero or size is greater than Integer.MAX_VALUE
     */
    @Throws(IOException::class)
    fun toByteArray(
        input: InputStream,
        size: Long,
    ): ByteArray {
        require(size <= Int.MAX_VALUE) { "Size cannot be greater than Integer max value: $size" }
        return toByteArray(input, size.toInt())
    }

    /**
     * Gets the contents of an [InputStream] as a [ByteArray].
     * Use this method instead of [toByteArray] when [InputStream] size is known.
     *
     * @param input the [InputStream] to read from
     * @param size  the size of [InputStream]
     * @return the requested byte array
     * @throws IOException if an I/O error occurs or [InputStream] size differ from parameter size
     * @throws IllegalArgumentException if size is less than zero
     */
    @Throws(IOException::class)
    fun toByteArray(
        input: InputStream,
        size: Int,
    ): ByteArray {
        require(size >= 0) { "Size must be equal or greater than zero: $size" }
        if (size == 0) return EMPTY_BYTE_ARRAY

        val data = ByteArray(size)
        var offset = 0

        while (offset < size) {
            val bytesRead = input.read(data, offset, size - offset)
            if (bytesRead == EOF) {
                break
            }
            offset += bytesRead
        }

        if (offset != size) {
            throw IOException("Unexpected read size. current: $offset, expected: $size")
        }

        return data
    }

    /**
     * Copies bytes from an [InputStream] to an [OutputStream].
     * This method buffers the input internally, so there is no need to use a [BufferedInputStream].
     *
     * @param input the [InputStream] to read from
     * @param output the [OutputStream] to write to
     * @return the number of bytes copied, or -1 if > Integer.MAX_VALUE
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun copy(
        input: InputStream,
        output: OutputStream,
    ): Int {
        val count = copyLarge(input, output)
        return if (count > Int.MAX_VALUE) -1 else count.toInt()
    }

    /**
     * Copies bytes from an [InputStream] to an [OutputStream] using an internal buffer of the given size.
     * This method buffers the input internally, so there is no need to use a [BufferedInputStream].
     *
     * @param input the [InputStream] to read from
     * @param output the [OutputStream] to write to
     * @param bufferSize the bufferSize used to copy from the input to the output
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun copy(
        input: InputStream,
        output: OutputStream,
        bufferSize: Int,
    ): Long {
        return copyLarge(input, output, ByteArray(bufferSize))
    }

    /**
     * Copies bytes from a large (over 2GB) [InputStream] to an [OutputStream].
     * This method buffers the input internally, so there is no need to use a [BufferedInputStream].
     * The buffer size is given by [DEFAULT_BUFFER_SIZE].
     *
     * @param input the [InputStream] to read from
     * @param output the [OutputStream] to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun copyLarge(
        input: InputStream,
        output: OutputStream,
    ): Long {
        return copy(input, output, DEFAULT_BUFFER_SIZE)
    }

    /**
     * Copies bytes from a large (over 2GB) [InputStream] to an [OutputStream].
     * This method uses the provided buffer, so there is no need to use a [BufferedInputStream].
     *
     * @param input the [InputStream] to read from
     * @param output the [OutputStream] to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun copyLarge(
        input: InputStream,
        output: OutputStream,
        buffer: ByteArray,
    ): Long {
        var count: Long = 0
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != EOF) {
            output.write(buffer, 0, bytesRead)
            count += bytesRead.toLong()
        }
        return count
    }
}
