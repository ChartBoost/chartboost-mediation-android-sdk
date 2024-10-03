package com.chartboost.sdk.internal.AdUnitManager.assets

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoaderCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.video.repository.VideoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class AssetsDownloaderTest {
    private val downloaderMock: Downloader = mockk(relaxed = true)
    private val callbackMock: AssetDownloadedCallback = mockk()
    private val adTraitsName = "adTraitsName"
    val timeSourceMock: TimeSource = mockk()
    val videoRepository: VideoRepository = mockk()
    val mediation: Mediation =
        Mediation(
            "mediation",
            "1.2.3",
            "3.2.1",
        )
    val adTraitsMock: AdType = mockk()
    val adUnitLoaderCallbackMock: AdUnitLoaderCallback = mockk()

    lateinit var assetsDownloader: AssetsDownloader

    @Before
    fun setup() {
        assetsDownloader =
            AssetsDownloaderImpl(downloaderMock, timeSourceMock, videoRepository, adTraitsMock, mediation)
        every { videoRepository.downloadVideoFile(any(), any(), any(), any()) } answers { }
    }

    @Test
    fun whenAppRequestAdUnitIsNullThenTheAssetsAreNotDownloaded() {
        val appRequest = appRequestStub
        appRequest.adUnit = null
        assetsDownloader.downloadAdUnitAssets(appRequest, adTraitsName, callbackMock, adUnitLoaderCallbackMock)
        verify(exactly = 0) { downloaderMock.resume() }
        verify(exactly = 0) { downloaderMock.downloadAssets(any(), any(), any(), any(), any()) }
    }

    @Test
    fun whenAppRequestIsNotDownloadingThenTheAssetsAreNotDownloaded() {
        val appRequest = appRequestStub
        appRequest.adUnit = null
        assetsDownloader.downloadAdUnitAssets(appRequest, adTraitsName, callbackMock, adUnitLoaderCallbackMock)
        verify(exactly = 0) { downloaderMock.resume() }
        verify(exactly = 0) { downloaderMock.downloadAssets(any(), any(), any(), any(), any()) }
    }

    @Test
    fun whenAppRequestIsDownloadingToShowAndAppRequestThenTheAssetsAreDownloaded() {
        val appRequest = appRequestStub
        appRequest.adUnit = AdUnit()

        assetsDownloader.downloadAdUnitAssets(appRequest, adTraitsName, callbackMock, adUnitLoaderCallbackMock)
        verify(exactly = 1) { downloaderMock.resume() }
        verify(exactly = 1) { downloaderMock.downloadAssets(any(), any(), any(), any(), any()) }
    }

    val appRequestStub: AppRequest
        get() {
            return AppRequest(1, "default", null)
        }
}
