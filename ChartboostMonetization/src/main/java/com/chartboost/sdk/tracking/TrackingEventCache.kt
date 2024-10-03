package com.chartboost.sdk.tracking

import android.content.SharedPreferences
import com.chartboost.sdk.internal.Model.asList
import com.chartboost.sdk.internal.Networking.requests.TrackingBodyBuilder
import com.chartboost.sdk.internal.logging.Logger
import org.json.JSONArray
import org.json.JSONObject

internal class TrackingEventCache(
    private val sharedPreferences: SharedPreferences,
    private val trackingBodyBuilder: TrackingBodyBuilder,
    private val jsonFactory: (json: String) -> JSONObject = { JSONObject(it) },
) {
    fun cacheEventJsonBodyAfterRequestFailure(jsonArray: JSONArray) {
        try {
            jsonArray.asList<JSONObject>().forEach {
                sharedPreferences.edit()
                    .putString(
                        generateCacheKeyFromJson(it),
                        it.toString(),
                    ).apply()
            }
        } catch (e: Exception) {
            Logger.d("cacheEventToTrackingRequestBodyAndSave error $e")
        }
    }

    fun cacheEventToTrackingRequestBodyAndSave(
        event: TrackingEvent,
        environmentData: EnvironmentData,
        persistenceMaxEvents: Int,
    ) {
        if (sharedPreferences.all.size > persistenceMaxEvents) {
            Logger.d("Persistence limit reached. Drop old events!")
            sharedPreferences.edit().clear().apply()
        }

        try {
            trackingBodyBuilder.buildTrackingRequestBody(event, environmentData).also {
                sharedPreferences.edit().putString(generateCacheKeyFromEvent(event), it).apply()
            }
        } catch (e: Exception) {
            Logger.d("cacheEventToTrackingRequestBodyAndSave error $e")
        }
    }

    fun eventsToTrackingRequestBodyJsonList(
        events: List<TrackingEvent>,
        environmentData: EnvironmentData,
    ): List<JSONObject> {
        return try {
            events.map {
                jsonFactory(
                    trackingBodyBuilder.buildTrackingRequestBody(
                        it,
                        environmentData,
                    ),
                )
            }
        } catch (e: Exception) {
            Logger.d("cacheEventToTrackingRequestBody error $e")
            emptyList()
        }
    }

    fun loadEventsAsJsonList(): List<JSONObject> {
        return try {
            sharedPreferences.all.values.toList().map {
                jsonFactory(it.toString()).also {
                    sharedPreferences.edit().clear().apply()
                }
            }
        } catch (e: Exception) {
            Logger.d("loadEventsAsJsonList error $e")
            emptyList()
        }
    }

    private fun generateCacheKeyFromJson(json: JSONObject): String {
        return json.getString("event_name") + json.getLong("event_timestamp")
    }

    private fun generateCacheKeyFromEvent(event: TrackingEvent): String {
        return event.name.value + event.timestamp
    }

    fun forcePersistEvent(
        event: TrackingEvent,
        environmentData: EnvironmentData,
    ) {
        try {
            Logger.d("forcePersistEvent: ${event.name.value}")
            trackingBodyBuilder.buildTrackingRequestBody(event, environmentData).also {
                sharedPreferences.edit().putString(event.name.value, it).apply()
            }
        } catch (e: Exception) {
            Logger.d("forcePersistEvent error $e")
        }
    }

    fun clearEventFromStorage(event: TrackingEvent) {
        try {
            Logger.d("clearEventFromStorage: ${event.name.value}")
            sharedPreferences.edit().remove(event.name.value).apply()
        } catch (e: Exception) {
            Logger.d("clearEventFromStorage error $e")
        }
    }
}
