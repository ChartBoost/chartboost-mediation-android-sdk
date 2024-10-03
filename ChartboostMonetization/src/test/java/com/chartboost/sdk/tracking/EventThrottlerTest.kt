package com.chartboost.sdk.tracking

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EventThrottlerTest {
    private val throttler = EventThrottler(10, 30)

    @Test
    fun nullEventTest() {
        val event = throttler.throttle(null)
        assertNull(event)
    }

    @Test
    fun eventValidTest() {
        val event = mockk<TrackingEvent>()
        every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
        every { event.timestamp } returns System.currentTimeMillis()
        val throttledEvent = throttler.throttle(event)
        assertNotNull(throttledEvent)
    }

    @Test
    fun eventThrottledTest() {
        val event = mockk<TrackingEvent>()
        every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
        every { event.timestamp } returns 1L
        val throttledEvent = throttler.throttle(event)
        assertNotNull(throttledEvent)
    }

    @Test
    fun eventThrottledTooManyEventsTest() {
        for (i in 0 until 11) {
            val event = mockk<TrackingEvent>()
            every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            every { event.timestamp } returns 1L
            val throttledEvent = throttler.throttle(event)
            if (i < 10) {
                assertNotNull(throttledEvent)
            } else {
                assertNotNull(throttledEvent)
                assertEquals(
                    TrackingEventName.Misc.TOO_MANY_EVENTS.value,
                    throttledEvent?.name?.value,
                )
            }
        }
    }

    @Test
    fun eventThrottledDisabledTest() {
        for (i in 0 until 13) {
            val event = mockk<TrackingEvent>()
            every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            every { event.timestamp } returns 1L
            val throttledEvent = throttler.throttle(event)
            if (i < 10) {
                assertNotNull(throttledEvent)
            } else if (i == 10) {
                assertNotNull(throttledEvent)
                assertEquals("too_many_events", throttledEvent?.name?.value)
            } else {
                assertNull(throttledEvent)
            }
        }
    }

    @Test
    fun eventThrottledFewEventsAboveTheTimeLimitTest() {
        val timestamp = System.currentTimeMillis()
        for (i in 0 until 4) {
            val event = mockk<TrackingEvent>()
            every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            every { event.timestamp } returns timestamp + (6000 * i)
            val throttledEvent = throttler.throttle(event)
            assertNotNull(throttledEvent)
        }
    }

    @Test
    fun eventThrottledFewEventBelowTheTimeLimitTest() {
        val timestamp = System.currentTimeMillis()
        for (i in 0 until 4) {
            val event = mockk<TrackingEvent>()
            every { event.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            every { event.timestamp } returns timestamp + (4000 * i)
            val throttledEvent = throttler.throttle(event)
            assertNotNull(throttledEvent)
        }
    }

    @Test
    fun eventThrottledDifferentEventsTest() {
        val timestamp = System.currentTimeMillis()
        val event1 = mockk<TrackingEvent>()
        every { event1.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
        every { event1.timestamp } returns timestamp
        val event2 = mockk<TrackingEvent>()
        every { event2.name } returns TrackingEventName.Misc.CONFIG_REQUEST_ERROR
        every { event2.timestamp } returns timestamp

        val throttledEvent1 = throttler.throttle(event1)
        assertNotNull(throttledEvent1)
        assertEquals(event1.name, throttledEvent1?.name)
        val throttledEvent2 = throttler.throttle(event2)
        assertNotNull(throttledEvent2)
        assertEquals(event2.name, throttledEvent2?.name)
    }

    @Test
    fun eventThrottledMultipleBelowEventLimitTest() {
        val now = System.currentTimeMillis()
        for (i in 0 until 10) {
            val event =
                InfoEvent.instance(
                    TrackingEventName.Misc.TOO_MANY_EVENTS,
                    "test",
                    "",
                    "",
                    null,
                )
            val time = now + (1000 * i)
            event.timestamp = time
            val throttled = throttler.throttle(event)
            assertNotNull(throttled)
            assertNotNull(event.name.value, throttled?.name)
        }
    }

    @Test
    fun eventThrottledMultipleAboveEventLimitTest() {
        val now = System.currentTimeMillis()
        for (i in 0 until 100) {
            val event =
                InfoEvent.instance(
                    TrackingEventName.Misc.TOO_MANY_EVENTS,
                    "test",
                    "",
                    "",
                    null,
                )
            val time = now + (1000 * i)
            event.timestamp = time
            val throttled = throttler.throttle(event)
            if (i < 11) {
                assertNotNull(throttled)
                assertNotNull(event.name.value, throttled?.name)
            } else {
                assertNull(throttled)
            }
        }
    }

    @Test
    fun eventThrottledMultipleBelowWindowLimitTest() {
        val throttler = EventThrottler(10, 5)
        val now = System.currentTimeMillis()
        for (i in 0 until 100) {
            val event =
                InfoEvent.instance(
                    TrackingEventName.Misc.TOO_MANY_EVENTS,
                    "test",
                    "",
                    "",
                    null,
                )
            val time = now + (1000 * i)
            event.timestamp = time
            val throttled = throttler.throttle(event)
            assertNotNull(throttled)
            assertNotNull(event.name.value, throttled?.name)
        }
    }

    @Test
    fun eventThrottledMultipleAboveBelowWindowLimitTest() {
        val throttler = EventThrottler(10, 5)
        val now = System.currentTimeMillis()
        for (i in 0 until 100) {
            val event =
                InfoEvent.instance(
                    TrackingEventName.Misc.TOO_MANY_EVENTS,
                    "test",
                    "",
                    "",
                    null,
                )
            val time = now + (6000 * i)
            event.timestamp = time
            val throttled = throttler.throttle(event)
            assertNotNull(throttled)
            assertNotNull(event.name.value, throttled?.name)
        }
    }

    @Test
    fun eventThrottledMultipleTooManyEventsTest() {
        val now = System.currentTimeMillis()
        for (i in 0 until 100) {
            val event =
                InfoEvent.instance(
                    TrackingEventName.Misc.TOO_MANY_EVENTS,
                    "test",
                    "",
                    "",
                    null,
                )
            val time = now + (100 * i)
            event.timestamp = time
            val throttled = throttler.throttle(event)
            if (i < 10) {
                assertNotNull(throttled)
                assertNotNull(event.name.value, throttled?.name)
            } else if (i == 10) {
                assertNotNull(throttled)
                assertNotNull("too_many_events", throttled?.name)
            } else {
                assertNull(throttled)
            }
        }
    }
}
