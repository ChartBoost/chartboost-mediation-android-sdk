/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.utils.LogController
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

object EventTrackersUtil {
    /**
     * @param eventTrackersJson The raw JSON object from app config.
     * @return Parsed map of [TrackingEvent] to [ServerEventTracker] list.
     */
    fun compileEventTrackers(eventTrackersJson: JsonObject): Map<TrackingEvent, List<ServerEventTracker>> {
        val trackersMap = mutableMapOf<TrackingEvent, List<ServerEventTracker>>()

        TrackingEvent.entries.forEach { event ->
            eventTrackersJson[event.serverName]?.let { jsonElement ->
                runCatching {
                    Json { ignoreUnknownKeys = true }.decodeFromJsonElement<List<ServerEventTracker>>(jsonElement)
                }.onFailure { e ->
                    val error =
                        when (e) {
                            is SerializationException, is IllegalArgumentException ->
                                "Error parsing ${event.serverName} event trackers: ${e.message}"

                            else ->
                                "Unexpected error parsing ${event.serverName} event trackers: ${e.message}"
                        }
                    LogController.e(error.plus(e))
                }.getOrNull()?.let { trackersForEvent ->
                    // null objects are deliberately omitted
                    // empty array disables tracking for an event
                    if (trackersForEvent.isNotEmpty()) {
                        trackersMap[event] = trackersForEvent
                    }
                }
            }
        }

        return trackersMap
    }
}
