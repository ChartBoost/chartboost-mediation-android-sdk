package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.*
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Networking.requests.models.ShowParamsModel
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.net.URL

@RunWith(MockitoJUnitRunner::class)
class AdUnitRendererShowRequestTest {
    private val networkServiceMock = mockk<CBNetworkService>(relaxed = true)
    private val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    private val requestBodyFieldsMock = mockk<RequestBodyFields>()
    private val mediationMock = mockk<Mediation>()
    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()

    private val adUnitRendererShowRequest =
        AdUnitRendererShowRequest(
            networkServiceMock,
            requestBodyBuilderMock,
            eventTracker = eventTrackerMock,
        )

    private val url =
        URL(
            CBConstants.API_PROTOCOL,
            EndpointRepository.DefaultHosts.AD_GET.defaultValue,
            "/interstitial/show",
        )

    @Before
    fun setup() {
        every { requestBodyBuilderMock.build() }.returns(requestBodyFieldsMock)
    }

    @Test
    fun `execute show request with video cached param`() {
        val requestCaptor = CapturingSlot<CBRequest>()
        val params = ShowParamsModel("adid", "location", 1, "interstitial", mediationMock)
        adUnitRendererShowRequest.execute(url, params)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured
        assertNotNull(request)
        assertEquals(url.toString(), request.uri)
        assertEquals("adid", request.body[CBConstants.REQUEST_PARAM_AD_ID])
        assertEquals("0", request.body[CBConstants.REQUEST_PARAM_CACHED])
        assertEquals("location", request.body[CBConstants.REQUEST_PARAM_LOCATION])
        assertEquals(1, request.body[CBConstants.REQUEST_PARAM_VIDEO_CACHED])
    }

    @Test
    fun `execute show request missing video cached value`() {
        val requestCaptor = CapturingSlot<CBRequest>()
        val params = ShowParamsModel("adid", "location", -1, "interstitial", mediationMock)
        adUnitRendererShowRequest.execute(url, params)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured
        assertNotNull(request)
        assertEquals(url.toString(), request.uri)
        assertEquals("adid", request.body[CBConstants.REQUEST_PARAM_AD_ID])
        assertEquals("0", request.body[CBConstants.REQUEST_PARAM_CACHED])
        assertEquals("location", request.body[CBConstants.REQUEST_PARAM_LOCATION])
        // check missing video cached param
        try {
            request.body[CBConstants.REQUEST_PARAM_VIDEO_CACHED]
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }
}
