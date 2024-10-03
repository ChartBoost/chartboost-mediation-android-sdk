package com.chartboost.sdk.internal.Networking

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBUtility
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.requests.TrackingRequest
import com.chartboost.sdk.internal.Networking.requests.TrackingRequestExtension
import com.chartboost.sdk.tracking.EventTracker
import com.chartboost.sdk.tracking.TrackingEvent
import com.chartboost.sdk.tracking.TrackingEventCache
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TrackingRequestTest {
    private val endpoint = "https://chartboost.com/track"
    private val eventMock = mockk<TrackingEvent>()
    private var events = listOf<JSONObject>()
    private var networkServiceMock = mockk<CBNetworkService>(relaxed = true)
    private var trackingEventCacheMock = mockk<TrackingEventCache>()
    private val eventTrackerMock =
        mockk<EventTracker>().apply {
            justRun { track(any()) }
        }
    private var request =
        TrackingRequest(
            networkServiceMock,
            trackingEventCacheMock,
            eventTracker = eventTrackerMock,
        )

    @Before
    fun setup() {
        every { eventMock.toString() } returns "{}"
        events = listOf(JSONObject(eventMock.toString()))
        every { eventMock.type } returns TrackingEvent.Type.ERROR
        every { networkServiceMock.submit(any<TrackingRequestExtension>()) } just Runs
        every { trackingEventCacheMock.cacheEventJsonBodyAfterRequestFailure(any()) } just Runs
    }

    @Test
    fun `execute test`() {
        val captor = CapturingSlot<TrackingRequestExtension>()
        request.execute(endpoint, events)
        verify(exactly = 1) { networkServiceMock.submit(capture(captor)) }
        val requestFinal = captor.captured
        val info = requestFinal.buildRequestInfo()
        Assert.assertNotNull(info)
        Assert.assertNotNull(info.body)
        Assert.assertTrue(info.body.isNotEmpty())
        Assert.assertEquals(3, info.headers.size.toLong())
        Assert.assertEquals(
            CBConstants.REQUEST_PARAM_HEADER_VALUE,
            info.headers[CBConstants.REQUEST_PARAM_ACCEPT_HEADER_KEY],
        )
        Assert.assertEquals(
            CBUtility.getUserAgent(),
            info.headers[CBConstants.REQUEST_PARAM_CLIENT_HEADER_KEY],
        )
        Assert.assertEquals(
            CBConstants.API_VERSION,
            info.headers[CBConstants.REQUEST_PARAM_HEADER_KEY],
        )
    }

    @Test
    fun `request failure`() {
        val extension = TrackingRequestExtension(endpoint, trackingEventCacheMock, eventTracker = eventTrackerMock)
        val jsonArray = JSONArray(listOf(JSONObject("{test : 1}")))
        extension.bodyArray = jsonArray
        extension.deliverError(
            CBError(CBError.Internal.HTTP_NOT_FOUND, "test error"),
            CBNetworkServerResponse(402, byteArrayOf()),
        )
        verify(exactly = 1) { trackingEventCacheMock.cacheEventJsonBodyAfterRequestFailure(jsonArray) }
    }
}
