package com.chartboost.sdk.internal.AssetLoader

import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyFields
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.defaultUrl
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTracker
import com.chartboost.sdk.tracking.TrackingEventName
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO Add more tests once parent class is moved to kotlin and refactored as current
 *      implementation is not fully testable and previous tests were abusing unit test environment
 */
class PrefetcherTest {
    private val downloaderMock = mockk<Downloader>()
    private val fileCacheMock = mockk<FileCache>()
    private val networkServiceMock = mockk<CBNetworkService>(relaxed = true)
    private val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    private val sdkConfigurationMock = mockk<SdkConfiguration>()
    private val sdkConfigurationRef = AtomicReference(sdkConfigurationMock)
    private val eventTrackerMock = mockk<EventTracker>()
    private val requestBodyFieldsMock = mockk<RequestBodyFields>()
    private val endpointRepositoryMock =
        mockk<EndpointRepository> {
            every { getEndPointUrl(any()) } answers { firstArg<EndpointRepository.EndPoint>().defaultUrl }
        }

    private val prefetcher =
        Prefetcher(
            downloaderMock,
            fileCacheMock,
            networkServiceMock,
            requestBodyBuilderMock,
            sdkConfigurationRef,
            eventTrackerMock,
            endpointRepositoryMock,
        )

    @Before
    fun setup() {
        every { networkServiceMock.submit(any<CBWebViewRequest>()) } just Runs
        every { sdkConfigurationMock.isWebviewEnabled } returns true
        every { sdkConfigurationMock.getPublisherDisable() } returns false
        every { sdkConfigurationMock.getPrefetchDisable() } returns false
        every { requestBodyBuilderMock.build() } returns requestBodyFieldsMock
        every { fileCacheMock.webViewCacheAssets } returns JSONObject()
        every { eventTrackerMock.track(any()) } just Runs
    }

    @Test
    fun `prefetcher cancel due to config publisher disabled is true`() {
        every { sdkConfigurationMock.getPublisherDisable() } returns true
        prefetcher.prefetch()
        verify(exactly = 0) { networkServiceMock.submit(any<CBRequest>()) }
    }

    @Test
    fun `prefetcher cancel due to config prefetch disabled is true`() {
        every { sdkConfigurationMock.getPrefetchDisable() } returns true
        prefetcher.prefetch()
        verify(exactly = 0) { networkServiceMock.submit(any<CBRequest>()) }
    }

    @Test
    fun `prefetcher send request`() {
        val captureSlot = CapturingSlot<CBWebViewRequest>()
        prefetcher.prefetch()
        verify(exactly = 1) { fileCacheMock.webViewCacheAssets }
        verify(exactly = 1) { networkServiceMock.submit(capture(captureSlot)) }
        val request = captureSlot.captured
        assertEquals(request.uri, EndpointRepository.EndPoint.PREFETCH.defaultUrl.toString())
        assertEquals(request.path, EndpointRepository.EndPoint.PREFETCH.defaultValue)
        assertEquals(request.body.toString(), "{\"ad\":{\"cache_assets\":{}}}")
    }

    @Test
    fun `prefetcher success`() {
        prefetcher.prefetch()
        prefetcher.onSuccess(mockk(), mockk())
        verify(exactly = 0) { downloaderMock.downloadAssets(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `prefetcher on failure`() {
        val errorSlot = CapturingSlot<ErrorEvent>()
        val error = mockk<CBError>()
        every { error.errorDesc } returns "prefetch error"
        prefetcher.prefetch()
        prefetcher.onFailure(mockk(), error)
        verify(exactly = 1) { eventTrackerMock.track(capture(errorSlot)) }
        assertEquals(TrackingEventName.Misc.PREFETCH_REQUEST_ERROR, errorSlot.captured.name)
        verify(exactly = 0) { downloaderMock.downloadAssets(any(), any(), any(), any(), any()) }
    }
}
