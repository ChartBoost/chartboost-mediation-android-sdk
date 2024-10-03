package com.chartboost.sdk.tracking

import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.TrackingConfig
import com.chartboost.sdk.internal.Networking.requests.TrackingRequest
import com.chartboost.sdk.internal.logging.Logger.d
import com.chartboost.sdk.privacy.PrivacyApi
import org.json.JSONObject

private const val LATENCY_ERROR = -1f
private const val NO_LATENCY = 0f

internal class EventTrackerImpl(
    private var config: Lazy<TrackingConfig>,
    private var throttler: Lazy<EventThrottler>,
    private var requestBodyBuilder: Lazy<RequestBodyBuilder>,
    private var privacyApi: Lazy<PrivacyApi>,
    private var environment: Lazy<Environment>,
    private var trackingRequest: Lazy<TrackingRequest>,
    private var trackingEventCache: Lazy<TrackingEventCache>,
) : EventTrackerExtensions, EventTracker {
    private val adsReference = mutableMapOf<String, TrackAd>()

    // This is only used to calculate latency
    private val references = mutableMapOf<String, TrackingEvent>()
    private val events = mutableListOf<TrackingEvent>()

    private val TrackAd.referenceKey: String
        get() = location + adType

    private val TrackingEvent.referenceKey: String
        get() = referenceKey(location, impressionAdType)

    private val environmentData: EnvironmentData
        get() =
            try {
                requestBodyBuilder.value.build().let {
                    environment.value.build(
                        it.identityBodyFields,
                        it.session,
                        it.reachabilityBodyFields.detailedConnectionType,
                        privacyApi.value,
                        it.REQUEST_PARAM_APP,
                    )
                }
            } catch (e: Exception) {
                d("Cannot create environment data for tracking", e)
                EnvironmentData()
            }

    override fun track(event: TrackingEvent) {
        with(config.value) {
            if (!isEnabled) {
                d("Tracking is disabled")
                return
            }

            if (blackList.contains(event.name)) {
                d("Event name ${event.name} is black-listed")
                return
            }
        }

        throttler.value.throttle(event)
            ?.let(::onEventTrackInBackground)
            ?: d("Event is throttled $event")
    }

    override fun persist(event: TrackingEvent) {
        event.apply {
            trackAd = adsReference[event.referenceKey]
            latency = calculateLatencyInSeconds()
            d("Persist event: $this")
        }

        trackingEventCache.value.forcePersistEvent(
            event,
            environmentData,
        )
    }

    override fun clearFromStorage(event: TrackingEvent) {
        trackingEventCache.value.clearEventFromStorage(event)
    }

    override fun clear(
        type: String,
        location: String,
    ) {
        references.remove(
            referenceKey(location, type),
        )
    }

    private fun referenceKey(
        location: String,
        type: String,
    ): String = location + type

    /**
     * Store ad information that will be attached to the event when applicable
     *
     * @param ad
     */
    override fun store(ad: TrackAd) {
        adsReference[ad.referenceKey] = ad
    }

    override fun refresh(config: TrackingConfig) {
        this.config = lazyOf(config)
    }

    private fun onEventTrackInBackground(event: TrackingEvent) {
        event.apply {
            trackAd = adsReference[event.referenceKey]
            latency = calculateLatencyInSeconds()
            callTrackingApi(this)
            d("Event: $this")
            saveReferenceStartEvent()
        }
    }

    private fun TrackingEvent.saveReferenceStartEvent() {
        if (isReferenceEvent()) {
            references[referenceKey] = this
        }
    }

    private fun TrackingEvent.isReferenceEvent(): Boolean {
        return when (name) {
            TrackingEventName.Cache.START,
            TrackingEventName.Show.START,
            -> true
            else -> false
        }
    }

    private fun TrackingEvent.calculateLatencyInSeconds(): Float {
        if (!shouldCalculateLatency) return latency
        if (!isLatencyEvent) return NO_LATENCY

        return try {
            references.remove(referenceKey)?.let { refEvent ->
                (timestamp - refEvent.timestamp) / 1000f
            } ?: LATENCY_ERROR
        } catch (e: Exception) {
            d("Cannot calculate latency", e)
            LATENCY_ERROR
        }
    }

    private fun callTrackingApi(event: TrackingEvent?) {
        try {
            event?.let {
                if (config.value.persistenceEnabled) {
                    executeRequestWithPersistence(it)
                } else {
                    executeRequestWithoutPersistence(it)
                }
            } ?: d("Cannot save empty event")
        } catch (e: Exception) {
            d("Cannot send tracking event", e)
        }
    }

    private fun executeRequestWithPersistence(event: TrackingEvent) {
        trackingEventCache.value.cacheEventToTrackingRequestBodyAndSave(
            event,
            environmentData,
            config.value.persistenceMaxEvents,
        )

        if (event.priority == TrackingEvent.Priority.HIGH) {
            executeTrackingRequest(trackingEventCache.value.loadEventsAsJsonList())
        }
    }

    private fun executeRequestWithoutPersistence(event: TrackingEvent) {
        events.add(event)
        if (event.priority == TrackingEvent.Priority.HIGH) {
            executeTrackingRequest(
                trackingEventCache.value.eventsToTrackingRequestBodyJsonList(
                    events,
                    environmentData,
                ),
            )
        }
    }

    private fun executeTrackingRequest(requestBody: List<JSONObject>) {
        trackingRequest.value.execute(
            config.value.endpoint,
            requestBody,
        )
    }

    override fun TrackingEvent.track(): TrackingEvent = apply(::track)

    override fun TrackAd.store(): TrackAd = apply(::store)

    override fun TrackingConfig.refresh(): TrackingConfig = apply(::refresh)

    override fun TrackingEvent.persist(): TrackingEvent = apply(::persist)

    override fun TrackingEvent.clearFromStorage(): TrackingEvent = apply(::clearFromStorage)
}
