package com.chartboost.sdk.internal.Networking

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.chartboost.sdk.test.mockAndroidLog
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CBImageDownloaderTest {
    private var mockUrl = mockk<URL>()
    private var mockConnection = mockk<HttpsURLConnection>()

    private var mockInputStream = mockk<InputStream>()
    private val mockBitmap = mockk<Bitmap>()
    private val testCoroutineScheduler = TestCoroutineScheduler()
    private val ioTestCoroutineDispatcher = StandardTestDispatcher(testCoroutineScheduler)

    private val expectedLogTag = "[ChartboostMonetization]"
    private val expectedMessage = "DispatchedTask.run(): Unable to download the info icon image"

    @Before
    fun setUp() {
        mockkStatic(BitmapFactory::class)

        mockAndroidLog()

        every { mockUrl.openConnection() } returns mockConnection

        every { mockConnection.inputStream } returns mockInputStream
        every { mockConnection.setDoInput(any()) } just Runs
        every { mockConnection.connect() } just Runs
        every { mockConnection.disconnect() } just Runs

        every { mockInputStream.read() } returns 0
        every { mockInputStream.close() } just Runs
    }

    @Test
    fun `download image successfully returns non-null bitmap`() {
        every { mockUrl.openConnection() } returns mockConnection

        val mockBitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap

        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { mockUrl },
                bitmapFactory = { mockBitmap },
            )
        var result: Bitmap? = null
        runTest(testCoroutineScheduler) {
            result = cbImageDownloader.downloadImage("validImageUrl")
        }

        assertEquals(mockBitmap, result)
    }

    @Test
    fun `download image handles connection timeout`() {
        every { mockConnection.inputStream } throws SocketTimeoutException()

        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { mockUrl },
                bitmapFactory = { null },
            )
        runTest(testCoroutineScheduler) {
            cbImageDownloader.downloadImage("validImageUrl")
            verify { Log.w(expectedLogTag, expectedMessage, any<SocketTimeoutException>()) }
        }
    }

    @Test
    fun `download image handles malformed URL`() {
        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { throw MalformedURLException() },
                bitmapFactory = { null },
            )
        runTest(testCoroutineScheduler) {
            cbImageDownloader.downloadImage("validImageUrl")
            verify { Log.w(expectedLogTag, expectedMessage, any<MalformedURLException>()) }
        }
    }

    @Test
    fun `download image handles IOException`() {
        every { mockConnection.inputStream } throws IOException()

        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { throw MalformedURLException() },
                bitmapFactory = { null },
            )
        runTest(testCoroutineScheduler) {
            cbImageDownloader.downloadImage("validImageUrl")
            verify { Log.w(expectedLogTag, expectedMessage, any<IOException>()) }
        }
    }

    @Test
    fun `download image handles null bitmap`() {
        every { BitmapFactory.decodeStream(any()) } returns null

        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { mockUrl },
                bitmapFactory = { null },
            )
        runTest(testCoroutineScheduler) {
            cbImageDownloader.downloadImage("validImageUrl")
            verify { Log.w(expectedLogTag, expectedMessage, any<IOException>()) }
        }
    }

    @Test
    fun `input and output streams are closed after operation`() {
        val cbImageDownloader =
            CBImageDownloader(
                ioDispatcher = ioTestCoroutineDispatcher,
                urlFactory = { mockUrl },
                bitmapFactory = { mockBitmap },
            )
        runTest(testCoroutineScheduler) {
            cbImageDownloader.downloadImage("validImageUrl")
        }

        verify { mockInputStream.close() }
        verify { mockConnection.disconnect() }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(BitmapFactory::class)
    }
}
