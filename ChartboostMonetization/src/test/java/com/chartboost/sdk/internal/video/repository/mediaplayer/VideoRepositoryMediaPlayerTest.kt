package com.chartboost.sdk.internal.video.repository.mediaplayer

import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.requests.VideoRequest
import com.chartboost.sdk.internal.utils.now
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.TempFileDownloadHelper
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_EMPTY
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_FULL
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_QUARTILE_1
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_QUARTILE_2
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_QUARTILE_3
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_QUARTILE_4
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.test.setPrivateField
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.ScheduledExecutorService

class VideoRepositoryMediaPlayerTest {
    private val networkMock = mockk<CBNetworkService>(relaxed = true)
    private val videoCachePolicyMock = mockk<VideoCachePolicy>()
    private val fileCacheMock = mockk<FileCache>(relaxed = true)
    private val reachabilityMock = mockk<CBReachability>()
    private val tempHelperMock = mockk<TempFileDownloadHelper>(relaxed = true)
    private val backgroundExecutorMock = mockk<ScheduledExecutorService>(relaxed = true)
    private val adUnitVideoPrecacheTempMock = mockk<AdUnitVideoPrecacheTemp>(relaxed = true)

    private lateinit var repository: VideoRepositoryMediaPlayer

    @Before
    fun setup() {
        videoCachePolicyMock.setPrivateField("timeWindowStartTimeStamp", 1L)
        justRun { videoCachePolicyMock.addDownloadToTimeWindow() }
        every { videoCachePolicyMock.isVideoCacheMaxSizeReached(any()) } returns true
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { videoCachePolicyMock.isFileTimeToLeave(any()) } returns true

        every { reachabilityMock.isNetworkAvailable } returns false

        every { networkMock.appId } returns "1"

        // No idea why we have to mock now() here like this, but removing it breaks deleteCacheFolderTest
        mockkStatic(::now)
        every { now() } answers { System.currentTimeMillis() }
    }

    @Test
    fun initWithEmptyStorageTest() {
        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset("0test.mp4")
        asset.shouldBeNull()
    }

    @Test
    fun initWithVideoFileInStorageTest() {
        val files = mockFilesInTheRepository(1)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            ).apply { initialize(mockk()) }
        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldNotBeNull()
    }

    @Test
    fun initWithTwoVideoFilesInStorageTest() {
        val files = mockFilesInTheRepository(2)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            ).apply { initialize(mockk()) }
        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldNotBeNull()
        val assetNext = repository.getVideoAsset(files[1]!!.name)
        assetNext.shouldNotBeNull()
    }

    @Test
    fun initWithVideoFileInStorageTimeToLeaveTest() {
        val fileCaptor = CapturingSlot<File>()
        val files = mockFilesInTheRepository(1, true)
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        verify(exactly = 1) {
            fileCacheMock!!.deleteFile(capture(fileCaptor))
        }
        val removedFile = fileCaptor.captured
        removedFile.name shouldBe files[0]!!.name
        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldBeNull()
    }

    @Test
    fun downloadVideoFileSuccessScheduleDownloadTest() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns false
        val requestCaptor = CapturingSlot<VideoRequest>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(url, filename, false, adUnitVideoPrecacheTempMock)
        verify(exactly = 1) {
            networkMock.submit(capture(requestCaptor))
        }
        val request = requestCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe url

        val assetAfterScheduleDownload = repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()
    }

    @Test
    fun downloadVideoFileCannotBeCachedNoSpaceTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(url, filename, false, adUnitVideoPrecacheTempMock)
        verify(exactly = 0) { networkMock.submit(any<VideoRequest>()) }
    }

    @Test
    fun downloadVideoFileCannotBeCachedWindowNotReachedTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(url, filename, false, adUnitVideoPrecacheTempMock)
        verify(exactly = 0) { networkMock.submit(any<VideoRequest>()) }
    }

    @Test
    fun downloadVideoFileWhileAlreadyDownloadingForceTest() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns false
        val requestNextCaptor = mutableListOf<VideoRequest>()

        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val urlNext = "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"

        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(url, filename, false, adUnitVideoPrecacheTempMock)
        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            true,
            adUnitVideoPrecacheTempMock,
        )

        verify(exactly = 2) { networkMock.submit(capture(requestNextCaptor)) }
        val request = requestNextCaptor[0]
        request.shouldNotBeNull()
        request.uri shouldBe url

        val requestNext = requestNextCaptor[1]
        requestNext.shouldNotBeNull()
        requestNext.uri shouldBe urlNext

        val assetAfterScheduleDownload = repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()

        val assetAfterScheduleDownloadNext = repository.getVideoAsset(filenameNext)
        assetAfterScheduleDownloadNext.shouldNotBeNull()
    }

    @Test
    fun downloadVideoFileWhileAlreadyDownloadingDontForceTest() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns false
        val requestNextCaptor = CapturingSlot<VideoRequest>()

        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val urlNext = "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"

        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(url, filename, false, adUnitVideoPrecacheTempMock)
        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            false,
            adUnitVideoPrecacheTempMock,
        )

        verify(exactly = 1) {
            networkMock.submit(capture(requestNextCaptor))
        }
        val request = requestNextCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe url

        val assetAfterScheduleDownload = repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()

        val assetAfterScheduleDownloadNext = repository.getVideoAsset(filenameNext)
        assetAfterScheduleDownloadNext.shouldNotBeNull()
    }

    @Test
    fun downloadVideoFileFileIsCachedCallbackWasNotRegisteredTest() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns false
        val urlCaptor = CapturingSlot<String>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val fileMock = relaxedMockk<File>()
        val dirRoot = spyk(File("./src/test/resources/cbtest/precache"))

        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { fileCacheMock.isFileCached(any()) } returns true
        every {
            fileCacheMock.getFileIfCached(
                any(),
                any(),
            )
        } returns fileMock
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        repository.downloadVideoFile(url, filename, true, adUnitVideoPrecacheTempMock)
        verify(exactly = 1) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(capture(urlCaptor))
        }
    }

    @Test
    fun downloadVideoFileFileIsCachedCallbackRegisteredTest() {
        every { tempHelperMock.isAssetDownloading(any(), any()) } returns false
        val urlCaptor = mutableListOf<String>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val fileMock = relaxedMockk<File>()
        val dirRoot = spyk(File("./src/test/resources/cbtest/precache"))

        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { fileCacheMock.isFileCached(any()) } returns true
        every {
            fileCacheMock.getFileIfCached(
                any(),
                any(),
            )
        } returns fileMock
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            url,
            filename,
            true,
            adUnitVideoPrecacheTempMock,
        )
        verify(exactly = 1) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(capture(urlCaptor))
        }

        repository.downloadVideoFile(
            url,
            filename,
            true,
            adUnitVideoPrecacheTempMock,
        )
        verify(exactly = 2) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(
                capture(urlCaptor),
            )
        }
    }

    @Test
    fun onSuccessWithoutNextScheduledTest() {
        val requestCaptor = CapturingSlot<VideoRequest>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"

        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.downloadVideoFile(
            url,
            filename,
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.onSuccess(url, filename)

        verify(exactly = 1) {
            networkMock.submit(capture(requestCaptor))
        }
        val request = requestCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe url

        val assetAfterScheduleDownload = repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()
    }

    @Test
    fun onSuccessWithoutNextScheduledOverTimeWindowTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"

        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }

        repository.downloadVideoFile(
            url,
            filename,
            false,
            adUnitVideoPrecacheTempMock,
        )

        repository.onSuccess(url, filename)

        verify(exactly = 0) { networkMock.submit(any<VideoRequest>()) }
    }

    @Test
    fun onSuccessWitNextScheduledTest() {
        val requestCaptor = mutableListOf<VideoRequest>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val urlNext = "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"

        val files = mockFilesInTheRepository(0)
        files.size shouldBe 0
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        val asset = repository.getVideoAsset(filename)
        asset.shouldBeNull()

        val dirRoot = spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }

        repository.downloadVideoFile(
            url,
            filename,
            true,
            adUnitVideoPrecacheTempMock,
        )
        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            true,
            adUnitVideoPrecacheTempMock,
        )

        repository.onSuccess(url, filename)

        verify(exactly = 2) { networkMock.submit(capture(requestCaptor)) }
        val request = requestCaptor[0]
        request.shouldNotBeNull()
        request.uri shouldBe url

        val assetAfterScheduleDownload = repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()

        val requestNext = requestCaptor[1]
        requestNext.shouldNotBeNull()
        requestNext.uri shouldBe urlNext

        val assetAfterScheduleDownloadNext =
            repository.getVideoAsset(filenameNext)
        assetAfterScheduleDownloadNext.shouldNotBeNull()
    }

    @Test
    fun onSuccessWithAssetInTheQueueTest() {
        every {
            tempHelperMock.isAssetDownloading(
                any(),
                any(),
            )
        } returns false
        val requestCaptor = CapturingSlot<VideoRequest>()
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val urlNext = "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"

        val file =
            spyk(File("./src/test/resources/cbtest/precache", filename))
        val fileNext =
            spyk(File("./src/test/resources/cbtest/precache", filenameNext))
        val files = arrayOf(file, fileNext)

        val dirMock = relaxedMockk<File>()
        every { fileCacheMock.precacheDir } answers { dirMock }

        files.size shouldBe 2
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
                tempHelper = tempHelperMock,
            )

        val dirRoot = spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true

        repository.downloadVideoFile(
            url,
            filename,
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            false,
            adUnitVideoPrecacheTempMock,
        )
        every { reachabilityMock.isNetworkAvailable } returns true
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        repository.onSuccess(url, filename)

        verify(exactly = 1) {
            networkMock.submit(capture(requestCaptor))
        }
        val request = requestCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe urlNext

        val assetAfterScheduleDownload =
            repository.getVideoAsset(filename)
        assetAfterScheduleDownload.shouldNotBeNull()
    }

    @Test
    fun onNullErrorTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(1)

        val error = null
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.initialize(mockk())
        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldNotBeNull()

        repository.onError(url, filename, error)
        verify(exactly = 1) {
            asset.localFile!!.delete()
        }

        val assetAfterError =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterError.shouldBeNull()
    }

    @Test
    fun onAnyErrorTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(1)

        val error =
            CBError(
                CBError.Internal.UNEXPECTED_RESPONSE,
                "test error",
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldNotBeNull()

        repository.onError(url, filename, error)
        verify(exactly = 1) {
            asset.localFile!!.delete()
        }

        val assetAfterError =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterError.shouldBeNull()
    }

    @Test
    fun onAnyErrorTriggerDownloadAgainTest() {
        val requestCaptor = CapturingSlot<VideoRequest>()

        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"

        val urlNext = "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"

        val files = mockFilesInTheRepository(2)

        val error =
            CBError(
                CBError.Internal.UNEXPECTED_RESPONSE,
                "test error",
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        val asset = repository.getVideoAsset(files[0]!!.name)
        asset.shouldNotBeNull()

        val assetNext =
            repository.getVideoAsset(files[1]!!.name)
        assetNext.shouldNotBeNull()

        val dirRoot =
            spyk(File("./src/test/resources/cbtest/videos"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false

        repository.downloadVideoFile(
            url,
            filename,
            true,
            adUnitVideoPrecacheTempMock,
        )
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true

        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            false,
            adUnitVideoPrecacheTempMock,
        )
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository.onError(url, filename, error)

        verify(exactly = 1) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(url)
        }

        // File was removed if we endup with map of size 1 and only video 1 is in the map
        val assetAfterDownload =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterDownload.shouldBeNull()

        val assetNextAfterDownload =
            repository.getVideoAsset(files[1]!!.name)
        assetNextAfterDownload.shouldNotBeNull()
        assetNextAfterDownload.localFile.shouldNotBeNull()

        verify(exactly = 1) {
            networkMock.submit(capture(requestCaptor))
        }
        val request = requestCaptor.captured
        request.shouldNotBeNull()
    }

    @Test
    fun onNoConnectionErrorTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(1)
        val error =
            CBError(
                CBError.Internal.INTERNET_UNAVAILABLE,
                "no connection test",
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        val asset =
            repository.getVideoAsset(files[0]!!.name)
        repository.onError(url, filename, error)
        verify(exactly = 1) {
            asset!!.localFile!!.delete()
        }
        val assetAfterError =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterError.shouldBeNull()
    }

    @Test
    fun onMiscellaneousErrorTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(1)
        val error =
            CBError(
                CBError.Internal.MISCELLANEOUS,
                "misc",
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        val asset =
            repository.getVideoAsset(files[0]!!.name)
        repository.onError(url, filename, error)
        verify(exactly = 1) {
            asset!!.localFile!!.delete()
        }
        val assetAfterError =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterError.shouldBeNull()
    }

    @Test
    fun onUnexpectedResponseErrorTest() {
        val url = "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val files = mockFilesInTheRepository(1)
        val error =
            CBError(
                CBError.Internal.UNEXPECTED_RESPONSE,
                "unexpected",
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply { initialize(mockk()) }

        val asset =
            repository.getVideoAsset(files[0]!!.name)
        repository.onError(url, filename, error)
        verify(exactly = 1) {
            asset!!.localFile!!.delete()
        }
        val assetAfterError =
            repository.getVideoAsset(files[0]!!.name)
        assetAfterError.shouldBeNull()
    }

    @Test
    fun deleteCacheFolderTest() {
        val requestCaptor =
            mutableListOf<VideoRequest>()
        val url =
            "http://test.com/0test.mp4"
        val filename = "0test.mp4"
        val urlNext =
            "http://test.com/1test.mp4"
        val filenameNext = "1test.mp4"
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        val policy =
            spyk(
                VideoCachePolicy(
                    maxBytes = 2L,
                    reachability = reachabilityMock,
                ),
            )

        every {
            fileCacheMock.getFolderSize(
                any(),
            )
        } returns 2
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } answers { true }

        every {
            policy.isVideoCacheMaxSizeReached(
                any(),
            )
        } returns true
        every { fileCacheMock.deleteFile(any()) } answers {
            every {
                policy.isVideoCacheMaxSizeReached(
                    any(),
                )
            } returns false
            true
        }

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                policy,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns false
        repository.downloadVideoFile(
            url,
            filename,
            true,
            adUnitVideoPrecacheTempMock,
        )
        Thread.sleep(100)
        repository.downloadVideoFile(
            urlNext,
            filenameNext,
            true,
            adUnitVideoPrecacheTempMock,
        )
        Thread.sleep(100)
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns true
        repository.onSuccess(url, filename)

        verify(exactly = 2) {
            networkMock.submit(
                capture(requestCaptor),
            )
        }
        val request =
            requestCaptor[1]
        request.shouldNotBeNull()
        request.uri shouldBe urlNext

        val assetAfterScheduleDownload =
            repository.getVideoAsset(
                filename,
            )
        assetAfterScheduleDownload.shouldBeNull()

        val assetAfterScheduleDownloadNext =
            repository.getVideoAsset(
                filenameNext,
            )
        assetAfterScheduleDownloadNext.shouldNotBeNull()
    }

    @Test
    fun removeAssetNullTest() {
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(null)
        isRemoved.shouldBeFalse()
    }

    @Test
    fun removeAssetLocalFileIsNullTest() {
        val dirFile = relaxedMockk<File>()
        val asset =
            VideoAsset(
                "http://test.com/video.mp4",
                "video.mp4",
                null,
                dirFile,
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(asset)
        isRemoved.shouldBeFalse()
    }

    @Test
    fun removeAssetIsDownloadingTest() {
        val dirFile = relaxedMockk<File>()
        val localFile = relaxedMockk<File>()
        val asset =
            VideoAsset(
                "http://test.com/video.mp4",
                "video.mp4",
                localFile,
                dirFile,
            )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(asset)
        isRemoved.shouldBeFalse()
    }

    @Test
    fun removeAssetIsNotDownloadingNotCachedTest() {
        val dirFile = relaxedMockk<File>()
        val localFile = relaxedMockk<File>()
        val asset =
            VideoAsset(
                "http://test.com/video.mp4",
                "video.mp4",
                localFile,
                dirFile,
            )
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns false
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(asset)
        isRemoved.shouldBeFalse()
    }

    @Test
    fun removeAssetIsNotDownloadingCachedButFailedToRemoveTest() {
        val dirFile = relaxedMockk<File>()
        val localFile = relaxedMockk<File>()
        val asset =
            VideoAsset(
                "http://test.com/video.mp4",
                "video.mp4",
                localFile,
                dirFile,
            )
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns true
        every { fileCacheMock.deleteFile(any()) } returns false
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(asset)
        isRemoved.shouldBeFalse()
    }

    @Test
    fun removeAssetIsNotDownloadingCachedAndFileRemovedTest() {
        val dirFile = relaxedMockk<File>()
        val localFile = relaxedMockk<File>()
        val asset =
            VideoAsset(
                "http://test.com/video.mp4",
                "video.mp4",
                localFile,
                dirFile,
            )
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns true
        every { fileCacheMock.deleteFile(any()) } returns true
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelperMock,
                backgroundExecutorMock,
            )
        val isRemoved =
            repository.removeAsset(asset)
        (isRemoved).shouldBeTrue()
    }

    @Test
    fun startDownloadIfPossibleNoNetworkTest() {
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )

        verify(exactly = 2) {
            backgroundExecutorMock.schedule(
                any(),
                any<Long>(),
                any(),
            )
        }
    }

    @Test
    fun startDownloadIfPossibleMaxCountForTimeWindowReached() {
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true
        every { reachabilityMock.isNetworkAvailable } returns true

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )

        verify(exactly = 2) {
            backgroundExecutorMock.schedule(
                any(),
                any<Long>(),
                any(),
            )
        }
    }

    @Test
    fun startDownloadIfPossibleForceDownloadTest() {
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns true
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { reachabilityMock.isNetworkAvailable } returns true

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            "0test.mp4",
            1,
            true,
        )

        verify(exactly = 1) {
            backgroundExecutorMock.schedule(
                any(),
                any<Long>(),
                any(),
            )
        }
    }

    @Test
    fun startDownloadIfPossibleAlreadyDownloadingDontForceTest() {
        val videoRequestCaptor =
            CapturingSlot<VideoRequest>()
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )
        repository.downloadVideoFile(
            "http://test.com/1test.mp4",
            "1test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )

        verify(exactly = 1) {
            networkMock.submit(
                capture(videoRequestCaptor),
            )
        }
        val request =
            videoRequestCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe "http://test.com/0test.mp4"
    }

    @Test
    fun startDownloadIfPossibleAlreadyDownloadingForceTest() {
        val videoRequestCaptor =
            mutableListOf<VideoRequest>()
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        every { videoCachePolicyMock.isMaxCountForTimeWindowReached() } returns false
        every { reachabilityMock.isNetworkAvailable } returns true

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            false,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )
        repository.downloadVideoFile(
            "http://test.com/1test.mp4",
            "1test.mp4",
            true,
            adUnitVideoPrecacheTempMock,
        )
        repository.startDownloadIfPossible(
            null,
            1,
            false,
        )

        verify(exactly = 2) {
            networkMock.submit(
                capture(videoRequestCaptor),
            )
        }
        val request =
            videoRequestCaptor[0]
        request.shouldNotBeNull()
        request.uri shouldBe "http://test.com/0test.mp4"
    }

    @Test
    fun startDownloadIfPossibleTest() {
        val videoRequestCaptor =
            CapturingSlot<VideoRequest>()
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }

        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            true,
            adUnitVideoPrecacheTempMock,
        )

        verify(exactly = 1) { videoCachePolicyMock.addDownloadToTimeWindow() }
        verify(exactly = 1) {
            networkMock.submit(
                capture(videoRequestCaptor),
            )
        }
        val request =
            videoRequestCaptor.captured
        request.shouldNotBeNull()
        request.uri shouldBe "http://test.com/0test.mp4"
    }

    @Test
    fun startDownloadIfPossibleEmptyQueueTest() {
        val videoRequestCaptor =
            CapturingSlot<VideoRequest>()
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
                tempHelper = tempHelperMock,
            )
        repository.startDownloadIfPossible(
            "0test.mp4",
            1,
            true,
        )
        verify(exactly = 0) {
            networkMock.submit(
                capture(videoRequestCaptor),
            )
        }
        verify(exactly = 0) {
            networkMock.submit(
                capture(videoRequestCaptor),
            )
        }
        verify(exactly = 0) {
            backgroundExecutorMock.schedule(
                any(),
                any<Long>(),
                any(),
            )
        }
    }

    @Test
    fun tempFileIsReadyVideoFileIsCreatedTest() {
        every {
            tempHelperMock.isAssetDownloading(
                any(),
                any(),
            )
        } returns false
        val urlCaptor1 =
            CapturingSlot<String>()
        val dirRoot =
            spyk(File("./src/test/resources/cbtest/precache"))
        every { fileCacheMock.precacheDir } answers { dirRoot }
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
                tempHelper = tempHelperMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            true,
            adUnitVideoPrecacheTempMock,
        )
        repository.tempFileIsReady(
            "http://test.com/0test.mp4",
            "0test.mp4",
            100,
            null,
        )
        verify(exactly = 0) {
            tempHelperMock.createRandomAccessFile(
                any(),
            )
        }
        verify(exactly = 1) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(
                capture(urlCaptor1),
            )
        }
        urlCaptor1.captured shouldBe "http://test.com/0test.mp4"
    }

    @Test
    fun tempFileIsReadyVideoFileIsNullTest() {
        every {
            tempHelperMock.isAssetDownloading(
                any(),
                any(),
            )
        } returns false
        val urlCaptor1 =
            CapturingSlot<String>()
        val dirRoot =
            spyk(
                File(
                    "./src/test/resources/cbtest/precache",
                ),
            )
        every { fileCacheMock.precacheDir } answers { dirRoot }
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
                tempHelper = tempHelperMock,
            )
        repository.downloadVideoFile(
            "http://test.com/0test.mp4",
            "0test.mp4",
            true,
            adUnitVideoPrecacheTempMock,
        )
        repository.tempFileIsReady(
            "http://test.com/0test.mp4",
            "0test.mp4",
            100,
            null,
        )
        verify(
            exactly = 0,
        ) {
            tempHelperMock.createRandomAccessFile(
                any(),
            )
        }
        verify(
            exactly = 1,
        ) {
            adUnitVideoPrecacheTempMock.tempVideoFileIsReady(
                capture(urlCaptor1),
            )
        }
        urlCaptor1.captured shouldBe "http://test.com/0test.mp4"
    }

    @Test
    fun getVideoDownloadStateEmptyNullAssetTest() {
        mockFilesInTheRepository(
            1,
        )
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelper = tempHelperMock,
                backgroundExecutor = backgroundExecutorMock,
            )
        repository.tempFileIsReady(
            "http://test.com/0test.mp4",
            "0test.mp4",
            100,
            null,
        ) // sets the expected file size
        val state =
            repository.getVideoDownloadState(
                null,
            )
        state shouldBe 0
    }

    @Test
    fun getVideoDownloadStateQuarterAllTempFileTest() {
        every {
            tempHelperMock.isAssetDownloading(
                any(),
                any(),
            )
        } returns false

        mockFilesInTheRepository(
            1,
        )
        val tempFile =
            relaxedMockk<File>()

        for (i in 0..100) {
            every { tempFile.length() } returns i.toLong()
            every {
                tempHelperMock.getTempFile(
                    any(),
                    any(),
                )
            } returns tempFile
            repository =
                VideoRepositoryMediaPlayer(
                    networkMock,
                    videoCachePolicyMock,
                    reachabilityMock,
                    fileCacheMock,
                    tempHelper = tempHelperMock,
                    backgroundExecutor = backgroundExecutorMock,
                ).apply {
                    initialize(
                        mockk(),
                    )
                }

            val asset =
                repository.getVideoAsset(
                    "0test.mp4",
                )
            repository.tempFileIsReady(
                "http://test.com/0test.mp4",
                "0test.mp4",
                100,
                null,
            ) // sets the expected file size
            val state =
                repository.getVideoDownloadState(
                    asset,
                )
            val expectedState =
                when {
                    i == 0 -> VIDEO_STATE_EMPTY
                    i < 25 -> VIDEO_STATE_QUARTILE_1
                    i < 50 -> VIDEO_STATE_QUARTILE_2
                    i < 75 -> VIDEO_STATE_QUARTILE_3
                    i < 100 -> VIDEO_STATE_QUARTILE_4
                    else -> VIDEO_STATE_FULL
                }
            state shouldBe expectedState
        }
    }

    @Test
    fun getVideoDownloadStateFullTempFileTest() {
        mockFilesInTheRepository(
            1,
        )
        val tempFile =
            relaxedMockk<File>()
        every { tempFile.length() } returns 100
        every {
            tempHelperMock.getTempFile(
                any(),
                any(),
            )
        } returns tempFile
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                tempHelper = tempHelperMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply {
                initialize(
                    mockk(),
                )
            }

        val asset =
            repository.getVideoAsset(
                "0test.mp4",
            )
        repository.tempFileIsReady(
            "http://test.com/0test.mp4",
            "0test.mp4",
            100,
            null,
        )
        verify(
            exactly = 0,
        ) {
            tempHelperMock.createRandomAccessFile(
                any(),
            )
        }
        val state =
            repository.getVideoDownloadState(
                asset,
            )
        state shouldBe VIDEO_STATE_FULL
    }

    @Test
    fun getVideoDownloadStateFullFileCachedTest() {
        mockFilesInTheRepository(
            1,
        )
        every {
            fileCacheMock.isFileCached(
                any(),
            )
        } returns true
        repository =
            VideoRepositoryMediaPlayer(
                networkMock,
                videoCachePolicyMock,
                reachabilityMock,
                fileCacheMock,
                backgroundExecutor = backgroundExecutorMock,
            ).apply {
                initialize(
                    mockk(),
                )
            }
        val asset =
            repository.getVideoAsset(
                "0test.mp4",
            )
        val state =
            repository.getVideoDownloadState(
                asset,
            )
        state shouldBe VIDEO_STATE_FULL
    }

    private fun mockFilesInTheRepository(
        size: Int,
        ttl: Boolean = false,
    ): Array<File?> {
        val files =
            arrayOfNulls<File>(
                size,
            )
        val dirMock =
            mockk<File>(relaxed = true)

        for (i in 0 until size) {
            val filename =
                i.toString() + "test.mp4"
            val videoFile =
                spyk(
                    File(
                        "./src/test/resources/cbtest/videos",
                        filename,
                    ),
                )
            files[i] =
                videoFile
            every { videoFile.name } returns filename
            every {
                videoCachePolicyMock.isFileTimeToLeave(
                    videoFile,
                )
            } returns ttl
        }
        every { fileCacheMock.precacheDir } answers { dirMock }
        every { fileCacheMock.precacheFiles } answers { files }
        return files
    }
}
