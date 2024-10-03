package com.chartboost.sdk.tracking

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.OpenForTesting
import com.chartboost.sdk.internal.utils.TimeStampSeconds
import com.chartboost.sdk.internal.utils.asSeconds

@OpenForTesting
sealed class TrackingEvent(
    val name: TrackingEventName,
    val message: String,
    val impressionAdType: String,
    val location: String,
    val mediation: Mediation?,
    val type: Type,
    var trackAd: TrackAd? = TrackAd(),
    var isLatencyEvent: Boolean = false,
    var shouldCalculateLatency: Boolean = true,
    var timestamp: Long = System.currentTimeMillis(),
    var latency: Float = 0f,
    var priority: Priority,
) {
    val timestampInSeconds: TimeStampSeconds
        get() = timestamp.asSeconds()

    enum class Type {
        INFO,
        CRITICAL,
        ERROR,
    }

    enum class Priority {
        LOW,
        HIGH,
    }

    override fun toString(): String {
        return "TrackingEvent(" +
            "name=${name.value}, " +
            "message='$message', " +
            "impressionAdType='$impressionAdType', " +
            "location='$location', " +
            "mediation=$mediation, " +
            "type=$type, " +
            "trackAd=$trackAd, " +
            "isLatencyEvent=$isLatencyEvent, " +
            "shouldCalculateLatency=$shouldCalculateLatency, " +
            "timestamp=$timestamp, " +
            "latency=$latency, " +
            "priority=$priority, " +
            "timestampInSeconds=$timestampInSeconds)"
    }
}
