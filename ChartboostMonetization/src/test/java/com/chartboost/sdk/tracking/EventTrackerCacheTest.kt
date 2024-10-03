package com.chartboost.sdk.tracking

import android.content.SharedPreferences
import com.chartboost.sdk.internal.Networking.requests.TrackingBodyBuilder
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class EventTrackerCacheTest {
    private val sharedPreferencesMock = mockk<SharedPreferences>()
    private val sharedPreferencesEditorMock = mockk<SharedPreferences.Editor>()
    private val trackingBodyBuilderMock = mockk<TrackingBodyBuilder>()
    private val eventMock = mockk<TrackingEvent>()
    private val environmentDataMock = mockk<EnvironmentData>()
    private val jsonObjectFMock = mockk<(json: String) -> JSONObject>()
    private val jsonObjectMock = mockk<JSONObject>()
    private val jsonArrayMock = mockk<JSONArray>()

    private val trackingEventCache =
        TrackingEventCache(
            sharedPreferencesMock,
            trackingBodyBuilderMock,
            jsonObjectFMock,
        )

    @Before
    fun setup() {
        every { eventMock.name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
        every { eventMock.timestamp } returns 123456789

        every { trackingBodyBuilderMock.buildTrackingRequestBody(any(), any()) } returns "{}"
        every { sharedPreferencesMock.edit() } returns sharedPreferencesEditorMock
        every { sharedPreferencesMock.all } returns mutableMapOf<String, String>("testEvent123456789" to "{}")

        every { sharedPreferencesEditorMock.putString(any(), any()) } returns sharedPreferencesEditorMock
        every { sharedPreferencesEditorMock.apply() } just Runs
        every { sharedPreferencesEditorMock.clear() } returns sharedPreferencesEditorMock
        every { sharedPreferencesEditorMock.remove(any()) } returns sharedPreferencesEditorMock
        every { jsonObjectMock.toString() } returns "{test: 1}"
        every { jsonObjectMock.getString("event_name") } returns "event_test"
        every { jsonObjectMock.getLong("event_timestamp") } returns 1

        every { jsonObjectFMock.invoke(any()) } returns jsonObjectMock
        every { jsonArrayMock.length() } returns 2
        every { jsonArrayMock.get(any()) } returns jsonObjectMock
    }

    @Test
    fun `build Tracking Request Body`() {
        trackingEventCache.cacheEventToTrackingRequestBodyAndSave(
            eventMock,
            environmentDataMock,
            10,
        )
        verify(exactly = 1) { trackingBodyBuilderMock.buildTrackingRequestBody(eventMock, environmentDataMock) }
        verify(exactly = 1) { sharedPreferencesMock.edit() }
        verify(exactly = 1) { sharedPreferencesEditorMock.putString("too_many_events123456789", "{}") }
        verify(exactly = 1) { sharedPreferencesEditorMock.apply() }
    }

    @Test
    fun `load Events As Json List`() {
        val events = trackingEventCache.loadEventsAsJsonList()
        verify(exactly = 1) { sharedPreferencesMock.all }
        assertEquals(events[0].toString(), "{test: 1}")
        assertEquals(events.size, 1)
    }

    @Test
    fun `store events in shared preferences after request failure`() {
        trackingEventCache.cacheEventJsonBodyAfterRequestFailure(jsonArrayMock)
        verify(exactly = 2) { sharedPreferencesMock.edit() }
        verify(exactly = 2) { sharedPreferencesEditorMock.putString("event_test1", "{test: 1}") }
        verify(exactly = 2) { sharedPreferencesEditorMock.apply() }
    }

    @Test
    fun `force Persist Event`() {
        trackingEventCache.forcePersistEvent(eventMock, environmentDataMock)
        verify(exactly = 1) { sharedPreferencesMock.edit() }
        verify(exactly = 1) { sharedPreferencesEditorMock.putString("too_many_events", "{}") }
        verify(exactly = 1) { sharedPreferencesEditorMock.apply() }
    }

    @Test
    fun `clear forced event from storage`() {
        trackingEventCache.clearEventFromStorage(eventMock)
        verify(exactly = 1) { sharedPreferencesMock.edit() }
        verify(exactly = 1) { sharedPreferencesEditorMock.remove("too_many_events") }
        verify(exactly = 1) { sharedPreferencesEditorMock.apply() }
    }
}
