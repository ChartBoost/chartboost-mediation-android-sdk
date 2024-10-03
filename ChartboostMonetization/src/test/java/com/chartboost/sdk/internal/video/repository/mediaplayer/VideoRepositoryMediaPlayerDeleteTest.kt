package com.chartboost.sdk.internal.video.repository.mediaplayer

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.test.setPrivateField
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.gms.common.ShadowGoogleApiAvailability
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ScheduledExecutorService

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    shadows = [ShadowGoogleApiAvailability::class],
    sdk = [Build.VERSION_CODES.P],
)
class VideoRepositoryMediaPlayerDeleteTest {
    private var context: Context? = null
    private var cacheDir: File? = null
    private val networkMock = mockk<CBNetworkService>(relaxed = true)
    private val policyMock = mockk<VideoCachePolicy>()
    private val fileCacheMock =
        mockk<FileCache>(relaxed = true)
    private val reachabilityMock =
        mockk<CBReachability>().apply { every { isNetworkAvailable } returns false }
    private val tempFileHelperMock = mockk<TempFileDownloadHelper>()
    private val backgroundExecutorMock =
        mockk<ScheduledExecutorService>(relaxed = true)
    private var repository: VideoRepositoryMediaPlayer? = null
    private val adUnitVideoPrecacheTempMock =
        relaxedMockk<AdUnitVideoPrecacheTemp>()

    @Before
    fun setUp() {
        every { tempFileHelperMock.isAssetDownloading(any(), any()) } returns false
        every { policyMock.isMaxCountForTimeWindowReached() } returns true
        every { networkMock.appId } returns "1"
        context = ApplicationProvider.getApplicationContext()
        cacheDir = context!!.cacheDir
        every { fileCacheMock.precacheDir } returns cacheDir
    }

    @Test
    @Throws(IOException::class)
    fun loadVideos() {
        policyMock.setPrivateField("timeWindowStartTimeStamp", 1L)
        every { policyMock.addDownloadToTimeWindow() } just runs
        every { policyMock.isVideoCacheMaxSizeReached(any()) } returns true

        val url = "http://test.com/cb/video.mp4"
        val filename = "cb_video.mp4"
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policyMock,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        repository!!.downloadVideoFile(url, filename, true, adUnitVideoPrecacheTempMock)
        saveToFile(filename, "test data")
        repository!!.onSuccess(url, filename)
        val asset = repository!!.getVideoAsset(filename)
        Assert.assertNotNull(asset)
        Assert.assertNotNull(asset!!.localFile)
        Assert.assertTrue(asset.localFile!!.exists())
        Assert.assertTrue(asset.localFile!!.length() > 0)
        Assert.assertNotNull(asset.localFile!!.path)
    }

    @Ignore("TODO RJM - Need to detemine what the intent was for this test")
    @Test
    @Throws(IOException::class)
    fun deleteVideosWhenLimitIsReachedTest() {
        val url = "http://test.com/cb/0video.mp4"
        val filename = "cb_0video.mp4"
        val urlNext = "http://test.com/cb/1video.mp4"
        val filenameNext = "1cb_video.mp4"
        val policy = VideoCachePolicy(15, 10, 8, 18000, 15000, 7200, 3, reachabilityMock)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        repository!!.downloadVideoFile(url, filename, true, adUnitVideoPrecacheTempMock)
        saveToFile(filename, "test data") // 9b
        repository!!.downloadVideoFile(urlNext, filenameNext, true, adUnitVideoPrecacheTempMock)
        saveToFile(filenameNext, "test data next") // 14b
        every { fileCacheMock.getFolderSize(any()) } returns 23L
        every { fileCacheMock.isFileCached(any()) } returns true
        every { fileCacheMock.deleteFile(any()) } answers {
            true
        }
        every { fileCacheMock.getFolderSize(any()) } returns 14L

        repository!!.onSuccess(url, filename)
        val asset = repository!!.getVideoAsset(filename)
        Assert.assertNull(asset)
        val assetNext = repository!!.getVideoAsset(filenameNext)
        Assert.assertNotNull(assetNext)
        Assert.assertNotNull(assetNext!!.localFile)
        Assert.assertTrue(assetNext.localFile!!.exists())
        Assert.assertTrue(assetNext.localFile!!.length() > 0)
        Assert.assertNotNull(assetNext.localFile!!.path)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun load10FilesWithinLimitTest() {
        val policy = VideoCachePolicy(100, 10, 8, 18000, 15000, 7200, 3, reachabilityMock)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        val storedFiles: List<String> = storeFiles(10)
        for (filename in storedFiles) {
            val asset = repository!!.getVideoAsset(filename)
            Assert.assertNotNull(asset)
            Assert.assertNotNull(asset!!.localFile)
            Assert.assertTrue(asset.localFile!!.exists())
            Assert.assertTrue(asset.localFile!!.length() > 0)
            Assert.assertNotNull(asset.localFile!!.path)
        }
    }

    @Ignore("HB-8129")
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun load10FilesExceedLimitTest() {
        val policy = VideoCachePolicy(50, 10, 8, 18000, 15000, 7200, 3, reachabilityMock)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        val storedFiles: List<String> = storeFiles(10)
        for (i in 0..9) {
            val filename = storedFiles[i]
            val asset = repository!!.getVideoAsset(filename)
            if (i > 4) {
                Assert.assertNotNull(asset)
                Assert.assertNotNull(asset!!.localFile)
                Assert.assertTrue(asset.localFile!!.exists())
                Assert.assertTrue(asset.localFile!!.length() > 0)
                Assert.assertNotNull(asset.localFile!!.path)
            }
        }
    }

    @Ignore("HB-8129")
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun load10FilesInLimitAndThen5FileAboveLimitTest() {
        val policy = VideoCachePolicy(100, 10, 8, 18000, 15000, 7200, 3, reachabilityMock)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        val storedFiles = storeFiles(10)
        for (filename in storedFiles) {
            val asset = repository!!.getVideoAsset(filename)
            Assert.assertNotNull(asset)
            Assert.assertNotNull(asset!!.localFile)
            Assert.assertTrue(asset.localFile!!.exists())
            Assert.assertTrue(asset.localFile!!.length() > 0)
            Assert.assertNotNull(asset.localFile!!.path)
        }
        val storedFilesExtra: List<String> = storeFiles(10, 15)
        storedFiles.addAll(storedFilesExtra)
        for (i in 0..14) {
            val filename = storedFiles[i]
            val asset = repository!!.getVideoAsset(filename)
            if (i > 3) {
                Assert.assertNotNull(asset)
                Assert.assertNotNull(asset!!.localFile)
                Assert.assertTrue(asset.localFile!!.exists())
                Assert.assertTrue(asset.localFile!!.length() > 0)
                Assert.assertNotNull(asset.localFile!!.path)
            }
        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun load10FilesInLimitAndThen10SameFilesTest() {
        val policy = VideoCachePolicy(100, 10, 8, 18000, 15000, 7200, 3, reachabilityMock)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                tempFileHelperMock,
                backgroundExecutorMock,
            )
        val storedFiles = storeFiles(10)
        for (filename in storedFiles) {
            val asset = repository!!.getVideoAsset(filename)
            Assert.assertNotNull(asset)
            Assert.assertNotNull(asset!!.localFile)
            Assert.assertTrue(asset.localFile!!.exists())
            Assert.assertTrue(asset.localFile!!.length() > 0)
            Assert.assertNotNull(asset.localFile!!.path)
        }
        val storedFilesExtra: List<String> = storeFiles(10)
        storedFiles.addAll(storedFilesExtra)
        for (i in 0..19) {
            val filename = storedFiles[i]
            val asset = repository!!.getVideoAsset(filename)
            Assert.assertNotNull(asset)
            Assert.assertNotNull(asset!!.localFile)
            Assert.assertTrue(asset.localFile!!.exists())
            Assert.assertTrue(asset.localFile!!.length() > 0)
            Assert.assertNotNull(asset.localFile!!.path)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun storeFiles(size: Int): MutableList<String> {
        return storeFiles(0, size)
    }

    private var storedFiles = 0
    var size = 9L * storedFiles

    @Throws(IOException::class, InterruptedException::class)
    fun storeFiles(
        start: Int,
        until: Int,
    ): MutableList<String> {
        val files: MutableList<String> = ArrayList()

        every {
            fileCacheMock.getFileIfCached(
                any(),
                any<String>(),
            )
        } returns null

        every { fileCacheMock.isFileCached(any()) } returns true
        storedFiles = start + 1
        size = 9L * storedFiles

        every { fileCacheMock.deleteFile(any()) } answers {
            true
        }

        storedFiles--
        size = 9L * storedFiles
        every { fileCacheMock.getFolderSize(any()) } returns size
        for (i in start until until) {
            val url = "http://test.com/cb/" + i + "video.mp4"
            val filename = "cb_" + i + "video.mp4"
            repository!!.downloadVideoFile(url, filename, true, adUnitVideoPrecacheTempMock)
            saveToFile(filename, "test data") // 9b
            files.add(filename)
            repository!!.onSuccess(url, filename)
            storedFiles++
            size = 9L * storedFiles
            every { fileCacheMock.getFolderSize(any()) } returns size
        }
        storedFiles = 0
        return files
    }

    @Throws(IOException::class)
    fun saveToFile(
        filename: String?,
        contents: String?,
    ): String {
        Thread.sleep(100)
        val file = File(cacheDir, filename)
        val fileWriter = FileWriter(file, false)
        fileWriter.write(contents)
        fileWriter.close()
        file.setLastModified(System.currentTimeMillis())
        return file.absolutePath
    }
}
