package com.chartboost.sdk.tracking

import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.TrackingConfig
import com.chartboost.sdk.internal.Networking.requests.TrackingRequest
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.test.TestUtils
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class EventTrackerImplTest {
    private val throttlerMock = mockk<EventThrottler>()
    private val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    private val configMock = mockk<TrackingConfig>()
    private val privacyApiMock = mockk<PrivacyApi>()
    private val environmentMock = mockk<Environment>()
    private val trackingRequestMock = mockk<TrackingRequest>()
    private val trackingEventCacheMock = mockk<TrackingEventCache>()

    private val tracker =
        EventTrackerImpl(
            throttler = lazyOf(throttlerMock),
            requestBodyBuilder = lazyOf(requestBodyBuilderMock),
            config = lazyOf(configMock),
            privacyApi = lazyOf(privacyApiMock),
            environment = lazyOf(environmentMock),
            trackingRequest = lazyOf(trackingRequestMock),
            trackingEventCache = lazyOf(trackingEventCacheMock),
        )

    @Before
    fun setup() {
        val fields = TestUtils.createTestBodyFields()
        every { requestBodyBuilderMock.build() } returns fields
        every { trackingEventCacheMock.forcePersistEvent(any(), any()) } just Runs
        every { trackingEventCacheMock.clearEventFromStorage(any()) } just Runs
        every { configMock.blackList } returns listOf()
    }

    private fun insertReferenceEvent(
        name: TrackingEventName,
        type: String,
        location: String,
        timestamp: Long,
    ) {
        val referenceEvent: TrackingEvent = InfoEvent(name, "", type, location, null)
        referenceEvent.timestamp = timestamp
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
    }

    @Test
    fun trackEventConfigEnabledTest() {
        val event = createTrackingEventMock()
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 1) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEventConfigDisabledTest() {
        val event = createTrackingEventMock()
        every { configMock.isEnabled } returns false
        verify(exactly = 0) { throttlerMock.throttle(event) }
        val tracked = tracker.track(event)
        Assert.assertNotNull(tracked)
    }

    @Test
    fun saveAdForTrackingTest() {
        val event = createTrackingEventMock()
        val adMock = event.trackAd!!
        tracker.store(adMock)
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(event) } returns event
        verify(exactly = 1) { adMock.adType }
        verify(exactly = 1) { adMock.adType }
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 1) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyInterstitialCacheTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        val referenceEvent: TrackingEvent =
            InfoEvent(
                TrackingEventName.Cache.START,
                "",
                "interstitial",
                "test",
                null,
            )
        referenceEvent.timestamp = 50L
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "interstitial"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyInterstitialTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true

        val referenceEvent: TrackingEvent =
            InfoEvent(
                TrackingEventName.Show.START,
                "",
                "interstitial",
                "test",
                null,
            )
        referenceEvent.timestamp = 50L
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "interstitial"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyRewardedTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        val referenceEvent: TrackingEvent =
            InfoEvent(
                TrackingEventName.Show.START,
                "",
                "rewarded",
                "test",
                null,
            )
        referenceEvent.timestamp = 50L
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "rewarded"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyBannerTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        val referenceEvent: TrackingEvent =
            InfoEvent(TrackingEventName.Show.START, "", "banner", "test", null)
        referenceEvent.timestamp = 50L
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "banner"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyWrongTypeTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        val referenceEvent: TrackingEvent =
            InfoEvent(TrackingEventName.Show.START, "", "interstitial", "test", null)
        referenceEvent.timestamp = 50L
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "Banner"
        every { event.location } returns "test"
        every { configMock.isEnabled } returns true
        every { throttlerMock.throttle(referenceEvent) } returns referenceEvent
        tracker.track(referenceEvent)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEvenWithLatencyMultipleShowsTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "interstitial"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        every { configMock.isEnabled } returns true
        insertReferenceEvent(TrackingEventName.Show.START, "interstitial", "test", 50)
        insertReferenceEvent(TrackingEventName.Show.START, "interstitial", "test", 51)
        insertReferenceEvent(TrackingEventName.Show.START, "interstitial", "test", 52)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 4) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        verify(exactly = 5) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        verify(exactly = 6) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun clearTrackedEventByLocationTest() {
        val event = createTrackingEventMock()
        every { event.isLatencyEvent } returns true
        every { configMock.isEnabled } returns true
        every { event.impressionAdType } returns "interstitial"
        every { event.location } returns "test"
        every { event.timestamp } returns 100L
        insertReferenceEvent(TrackingEventName.Show.START, "interstitial", "test", 50)
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 2) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
        tracker.clear("interstitial", "test")
        every { throttlerMock.throttle(event) } returns event
        tracker.track(event)
        verify(exactly = 3) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    @Test
    fun trackEventConfigPersistenceDisabledTest() {
        val eventHigh =
            mockk<TrackingEvent>().apply {
                every { priority } returns TrackingEvent.Priority.HIGH
                every { name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
                every { location } returns "loc"
                every { impressionAdType } returns "interstitial"
                every { isLatencyEvent } returns false
                every { latency = 0f } just Runs
                every { trackAd = null } just Runs
                every { shouldCalculateLatency } returns true
            }

        val eventLow =
            mockk<TrackingEvent>().apply {
                every { priority } returns TrackingEvent.Priority.LOW
                every { name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
                every { location } returns "loc1"
                every { impressionAdType } returns "interstitial"
                every { isLatencyEvent } returns false
                every { latency = 0f } just Runs
                every { trackAd = null } just Runs
                every { shouldCalculateLatency } returns true
            }

        every { configMock.isEnabled } returns true
        every { configMock.persistenceEnabled } returns false
        every { throttlerMock.throttle(eventLow) } returns eventLow
        tracker.track(eventLow)
        verify(exactly = 0) {
            trackingEventCacheMock.eventsToTrackingRequestBodyJsonList(
                any(),
                any(),
            )
        }

        every { throttlerMock.throttle(eventHigh) } returns eventHigh
        tracker.track(eventHigh)
        verify(exactly = 1) {
            trackingEventCacheMock.eventsToTrackingRequestBodyJsonList(
                listOf(
                    eventLow,
                    eventHigh,
                ),
                any(),
            )
        }
    }

    @Test
    fun `force persist event test`() {
        val event = createTrackingEventMock()
        tracker.persist(event)
        verify(exactly = 1) { trackingEventCacheMock.forcePersistEvent(event, any()) }
    }

    @Test
    fun `clear event from forced persist`() {
        val event = createTrackingEventMock()
        tracker.clearFromStorage(event)
        verify(exactly = 1) { trackingEventCacheMock.clearEventFromStorage(event) }
    }

    @Test
    fun whenEventsDisabledBlackListShouldNotBeChecked() {
        val mockEvent = createTrackingEventMock()
        every { configMock.isEnabled } returns false
        tracker.track(mockEvent)
        verify(exactly = 0) { configMock.blackList }
    }

    @Test
    fun whenEventsEnabledBlacklistedEventsShouldNotBeTracked() {
        val mockEvent =
            mockk<TrackingEvent>().apply {
                every { name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            }
        every { configMock.isEnabled } returns true
        every { configMock.blackList } returns listOf(TrackingEventName.Misc.TOO_MANY_EVENTS)
        tracker.track(mockEvent)
        verify(exactly = 0) { throttlerMock.throttle(any()) }
    }

    @Test
    fun whenEventsEnabledNonBlacklistedEventsShouldBeTracked() {
        val mockEvent =
            createTrackingEventMock().apply {
                every { name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            }
        every { throttlerMock.throttle(any()) } returns mockEvent
        every { configMock.isEnabled } returns true
        every { configMock.blackList } returns listOf(TrackingEventName.Misc.IMPRESSION_RECORDED)
        tracker.track(mockEvent)
        val capturedEvents = mutableListOf<TrackingEvent>()
        verify(exactly = 1) { throttlerMock.throttle(capture(capturedEvents)) }
        Assert.assertNotNull(capturedEvents)
    }

    private fun createTrackingEventMock(): TrackingEvent {
        val mockTrackAd =
            mockk<TrackAd>().apply {
                every { location } returns "loc"
                every { adType } returns "banner"
            }

        return mockk<TrackingEvent>().apply {
            every { location } returns "loc"
            every { name } returns TrackingEventName.Show.START
            every { latency = any() } just runs
            every { trackAd = any() } just runs
            every { trackAd } returns mockTrackAd
            every { impressionAdType } returns "interstitial"
            every { shouldCalculateLatency } returns true
            every { latency = any() } just runs
            every { isLatencyEvent } returns true
        }
    }
}
