package com.chartboost.sdk.internal.Model

import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.tracking.asTrackingEventNames
import org.json.JSONObject

@VisibleForTesting const val TRACKING_KEY = "tracking"

@VisibleForTesting const val TRACKING_ENABLED_KEY = "enabled"

@VisibleForTesting const val TRACKING_ENDPOINT_KEY = "endpoint"

@VisibleForTesting const val TRACKING_EVENT_LIMIT_KEY = "eventLimit"

@VisibleForTesting const val TRACKING_WINDOW_DURATION_KEY = "windowDuration"

@VisibleForTesting const val TRACKING_PERSISTENCE_ENABLED_KEY = "persistenceEnabled"

@VisibleForTesting const val TRACKING_PERSISTENCE_MAX_EVENTS_KEY = "persistenceMaxEvents"

@VisibleForTesting const val TRACKING_BLACK_LIST_KEY = "blacklist"

@VisibleForTesting const val TRACKING_ENABLED_DEFAULT = false

@VisibleForTesting const val TRACKING_EVENT_LIMIT_DEFAULT = 10

@VisibleForTesting const val TRACKING_WINDOW_DURATION_DEFAULT = 60

@VisibleForTesting const val TRACKING_PERSISTENCE_ENABLED_DEFAULT = true

@VisibleForTesting const val TRACKING_PERSISTENCE_MAX_EVENTS_DEFAULT = 100

@VisibleForTesting val TRACKING_BLACKLIST_DEFAULT = emptyList<TrackingEventName>()

data class TrackingConfig(
    val isEnabled: Boolean = TRACKING_ENABLED_DEFAULT,
    val blackList: List<TrackingEventName> = TRACKING_BLACKLIST_DEFAULT,
    val endpoint: String = CBConstants.API_ENDPOINT_TRACKING_DEFAULT,
    val eventLimit: Int = TRACKING_EVENT_LIMIT_DEFAULT,
    val windowDuration: Int = TRACKING_WINDOW_DURATION_DEFAULT,
    val persistenceEnabled: Boolean = TRACKING_PERSISTENCE_ENABLED_DEFAULT,
    val persistenceMaxEvents: Int = TRACKING_PERSISTENCE_MAX_EVENTS_DEFAULT,
)

internal fun JSONObject.parseTrackingConfig(): TrackingConfig {
    return optJSONObject(TRACKING_KEY)?.run {
        TrackingConfig(
            isEnabled = optBoolean(TRACKING_ENABLED_KEY, TRACKING_ENABLED_DEFAULT),
            endpoint = optString(TRACKING_ENDPOINT_KEY, CBConstants.API_ENDPOINT_TRACKING_DEFAULT),
            eventLimit = optInt(TRACKING_EVENT_LIMIT_KEY, TRACKING_EVENT_LIMIT_DEFAULT),
            windowDuration = optInt(TRACKING_WINDOW_DURATION_KEY, TRACKING_WINDOW_DURATION_DEFAULT),
            persistenceEnabled = optBoolean(TRACKING_PERSISTENCE_ENABLED_KEY, TRACKING_PERSISTENCE_ENABLED_DEFAULT),
            persistenceMaxEvents = optInt(TRACKING_PERSISTENCE_MAX_EVENTS_KEY, TRACKING_PERSISTENCE_MAX_EVENTS_DEFAULT),
            blackList = blackList(),
        )
    } ?: TrackingConfig()
}

private fun JSONObject.blackList(): List<TrackingEventName> =
    optJSONArray(TRACKING_BLACK_LIST_KEY)
        ?.asList<String>()
        ?.asTrackingEventNames()
        ?: TRACKING_BLACKLIST_DEFAULT
