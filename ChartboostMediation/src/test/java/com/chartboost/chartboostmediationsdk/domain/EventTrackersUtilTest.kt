/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class EventTrackersUtilTest {
    @Test
    fun `compileEventTrackers returns parsed trackers correctly`() {
        val trackerUrl = "https://test.com/click"
        val json =
            buildJsonObject {
                put("click", Json.encodeToJsonElement(listOf(ServerEventTracker(trackerUrl))))
            }

        val result = EventTrackersUtil.compileEventTrackers(json)

        assertTrue(result.containsKey(TrackingEvent.CLICK))
        assertEquals(trackerUrl, result[TrackingEvent.CLICK]?.firstOrNull()?.url)
    }

    @Test
    fun `compileEventTrackers skips malformed trackers`() {
        val malformedJson =
            buildJsonObject {
                put("click", Json.parseToJsonElement("12345"))
            }

        val result = EventTrackersUtil.compileEventTrackers(malformedJson)

        assertFalse(result.containsKey(TrackingEvent.CLICK))
    }

    @Test
    fun `compileEventTrackers skips null event entries`() {
        val json =
            buildJsonObject {
                put("click", JsonNull)
            }

        val result = EventTrackersUtil.compileEventTrackers(json)

        assertFalse(result.containsKey(TrackingEvent.CLICK))
    }

    @Test
    fun `compileEventTrackers ignores unknown events`() {
        val json =
            buildJsonObject {
                put("non_existing_event", Json.encodeToJsonElement(listOf(ServerEventTracker("https://test.com"))))
            }

        val result = EventTrackersUtil.compileEventTrackers(json)

        assertFalse(result.containsKey(TrackingEvent.CLICK))
        assertFalse(result.containsKey(TrackingEvent.PARTNER_IMPRESSION))
    }
}
