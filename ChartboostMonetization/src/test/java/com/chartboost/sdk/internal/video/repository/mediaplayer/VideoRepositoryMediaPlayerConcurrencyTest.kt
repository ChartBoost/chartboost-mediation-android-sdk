package com.chartboost.sdk.internal.video.repository.mediaplayer

import android.content.Context
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.test.setPrivateField
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.ScheduledExecutorService

@OptIn(DelicateCoroutinesApi::class)
class VideoRepositoryMediaPlayerConcurrencyTest {
    private val networkMock = relaxedMockk<CBNetworkService>()
    private val policyMock = mockk<VideoCachePolicy>()
    private val fileCacheMock = relaxedMockk<FileCache>()
    private val reachabilityMock = mockk<CBReachability>()
    private val tempHelperMock = mockk<TempFileDownloadHelper>()
    private val backgroundExecutorMock = relaxedMockk<ScheduledExecutorService>()
    private val adUnitVideoPrecacheTempMock = relaxedMockk<AdUnitVideoPrecacheTemp>()
    private val contextMock = mockk<Context>()

    private lateinit var repository: VideoRepositoryMediaPlayer

    @Before
    fun init() {
        policyMock.setPrivateField("timeWindowStartTimeStamp", 1L)
        every { policyMock.addDownloadToTimeWindow() } just Runs
        every { policyMock.isVideoCacheMaxSizeReached(any()) } returns false
        every { fileCacheMock.precacheDir } returns
            relaxedMockk<File>().apply {
                every { path } returns "/"
            }

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
    }

    @Test
    fun accessToVideoQueConcurrentlyDoesNotThrowAnyException() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns true
        every { policyMock.isMaxCountForTimeWindowReached() } returns true
        every { reachabilityMock.isNetworkAvailable } returns true
        val url = "http://test.com/0test"
        val filename = "0test"
        val extension = ".mp4"
        runBlocking {
            val scope = CoroutineScope(newFixedThreadPoolContext(4, "synchronizationPool"))
            val handler =
                CoroutineExceptionHandler { _, _ ->
                    fail("exception has been handled")
                }
            val coroutines =
                1.rangeTo(8).map {
                    val urlIndexed = "$url$it$extension"
                    val fileIndexed = "$filename$it$extension"
                    scope.async(handler) {
                        repository.downloadVideoFile(
                            urlIndexed,
                            fileIndexed,
                            false,
                            adUnitVideoPrecacheTempMock,
                        )
                    }
                }
            coroutines.forEach { it.await() }
            assertTrue("No exception thrown", true)
            println("Jobs ended")
        }
    }

    @Test
    fun accessToVideoQueConcurrentlyWithErrorAndSuccessToAccessQueue() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns true
        every { policyMock.isMaxCountForTimeWindowReached() } returns true
        every { reachabilityMock.isNetworkAvailable } returns true
        val url = "http://test.com/0test"
        val filename = "0test"
        val extension = ".mp4"
        runBlocking {
            val scope = CoroutineScope(newFixedThreadPoolContext(4, "synchronizationPool"))
            val handler =
                CoroutineExceptionHandler { _, _ ->
                    fail("exception has been handled")
                }
            val coroutines =
                1.rangeTo(8).map {
                    val urlIndexed = "$url$it$extension"
                    val fileIndexed = "$filename$it$extension"

                    scope.async(handler) {
                        repository.onError(
                            urlIndexed,
                            fileIndexed,
                            CBError(CBError.Internal.MISCELLANEOUS, "test error"),
                        )
                        repository.downloadVideoFile(
                            urlIndexed,
                            fileIndexed,
                            false,
                            adUnitVideoPrecacheTempMock,
                        )
                        repository.onSuccess(urlIndexed, fileIndexed)
                    }
                }
            coroutines.forEach { it.await() }
            assertTrue("No exception thrown", true)
            println("Jobs ended")
        }
    }

    @Test
    fun accessToVideoMapConcurrentlyDoesNotThrowAnyException() {
        val numberOfFiles = 1000
        every { policyMock.isFileTimeToLeave(any()) } returns false
        val arrayOfTempFiles: Array<File> = generateRandomFiles(numberOfFiles)
        every { fileCacheMock.precacheFiles } answers { arrayOfTempFiles }
        every { fileCacheMock.isFileCached(any()) } answers { true }
        every { fileCacheMock.deleteFile(any()) } answers { true }

        runBlocking {
            val scope = CoroutineScope(newFixedThreadPoolContext(3, "synchronizationPool"))
            val coroutineForAdding =
                scope.async {
                    repository.initialize(contextMock)
                }
            val coroutineForAccessing =
                scope.async {
                    delay(100)
                    arrayOfTempFiles.forEach {
                        repository.getVideoAsset(it.name)
                    }
                }
            val coroutineDeleting =
                scope.async {
                    delay(100)
                    arrayOfTempFiles.forEach {
                        repository.removeAsset(repository.getVideoAsset(it.name))
                    }
                }
            coroutineDeleting.await()
            coroutineForAccessing.await()
            coroutineForAdding.await()
        }
        assertTrue("No exception thrown", true)
    }

    fun generateRandomFiles(numberOfFiles: Int): Array<File> {
        val files =
            1.rangeTo(numberOfFiles).map {
                File.createTempFile("temp_$it", ".txt")
            }
        return files.toTypedArray()
    }
}
