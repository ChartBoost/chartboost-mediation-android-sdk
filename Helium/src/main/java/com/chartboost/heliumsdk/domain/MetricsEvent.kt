package com.chartboost.heliumsdk.domain

/**
 * The metrics to be collected for an ad lifecycle event. Each entity is a single event that
 * corresponds to a single partner.
 *
 * @property eventType The type of event.
 * @property partner The partner name.
 */
data class MetricsEvent(val eventType: EventType, val partner: String) {
    /**
     * The type of event.
     */
    enum class EventType {
        INITIALIZATION, PREBID, LOAD, SHOW, CLICK, EXPIRATION
    }

    private val startTimestamp: Long = System.currentTimeMillis()
    var endTimestamp: Long = 0L
    var isSuccess: Boolean = false
    var error: ChartboostMediationError? = null

    // TODO: Port the other one-off fields from the [[Metrics]] class.

    fun getDuration() = if (endTimestamp > 0) endTimestamp - startTimestamp else 0
}
