package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.tracking.TrackingEventName
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class TrackingConfigTest {
    private val emptyTrackingJSONMock =
        mockk<JSONObject>().apply {
            every { optBoolean(any(), any()) } answers { secondArg() }
            every { optString(any(), any()) } answers { secondArg() }
            every { optInt(any(), any()) } answers { secondArg() }
            every { optJSONArray(any()) } returns null
        }

    @Test
    fun whenParsingJsonWithNoTrackingShouldReturnDefault() {
        val mockJSONObject =
            mockk<JSONObject>().apply {
                every { optJSONObject(any()) } returns null
            }
        mockJSONObject.parseTrackingConfig() shouldBe TrackingConfig()
    }

    @Test
    fun whenParsingJsonWithEmptyTrackingShouldReturnDefault() {
        val mockJSONObject =
            mockk<JSONObject>().apply {
                every { optJSONObject(TRACKING_KEY) } returns emptyTrackingJSONMock
            }
        mockJSONObject.parseTrackingConfig() shouldBe TrackingConfig()
    }

    @Test
    fun whenParsingJsonWithOnlySomeTrackingValuesShouldSetThoseValuesAndDefaultTheRest() {
        val isEnabled = !TRACKING_ENABLED_DEFAULT
        val eventLimit = TRACKING_EVENT_LIMIT_DEFAULT + 10
        val persistenceEnabled = !TRACKING_PERSISTENCE_ENABLED_DEFAULT
        val mockJSONObject =
            mockk<JSONObject>().apply {
                every { optJSONObject(TRACKING_KEY) } returns
                    emptyTrackingJSONMock.apply {
                        every { optBoolean(TRACKING_ENABLED_KEY, any()) } returns isEnabled
                        every { optInt(TRACKING_EVENT_LIMIT_KEY, any()) } returns eventLimit
                        every { optBoolean(TRACKING_PERSISTENCE_ENABLED_KEY, any()) } returns persistenceEnabled
                        every { optInt(TRACKING_WINDOW_DURATION_KEY, any()) } answers { secondArg() }
                        every { optInt(TRACKING_PERSISTENCE_MAX_EVENTS_KEY, any()) } answers { secondArg() }
                    }
            }
        mockJSONObject.parseTrackingConfig() shouldBe
            TrackingConfig(
                isEnabled = isEnabled,
                eventLimit = eventLimit,
                persistenceEnabled = persistenceEnabled,
            )
    }

    @Test
    fun whenParsingJsonEmptyBlacklistShouldSetEmptyBlacklist() {
        val jsonBlacklist = "[]"
        val mockJSONObject =
            mockk<JSONObject>().apply {
                every { optJSONObject(TRACKING_KEY) } returns
                    emptyTrackingJSONMock.apply {
                        every { optJSONArray(TRACKING_BLACK_LIST_KEY) } returns JSONArray(jsonBlacklist)
                    }
            }
        mockJSONObject.parseTrackingConfig() shouldBe
            TrackingConfig(
                blackList = emptyList(),
            )
    }

    @Test
    fun whenParsingJsonBlacklistWithInvalidEventsShouldSkipThem() {
        val jsonBlacklist = "[\"foo\",\"too_many_events\"]"
        val mockJSONObject =
            mockk<JSONObject>().apply {
                every { optJSONObject(TRACKING_KEY) } returns
                    emptyTrackingJSONMock.apply {
                        every { optJSONArray(TRACKING_BLACK_LIST_KEY) } returns JSONArray(jsonBlacklist)
                    }
            }
        mockJSONObject.parseTrackingConfig() shouldBe
            TrackingConfig(
                blackList = listOf(TrackingEventName.Misc.TOO_MANY_EVENTS),
            )
    }
}
