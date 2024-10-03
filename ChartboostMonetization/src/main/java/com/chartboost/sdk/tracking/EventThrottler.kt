package com.chartboost.sdk.tracking

import com.chartboost.sdk.OpenForTesting

private const val DEFAULT_EVENT_LIMIT = 10
private const val DEFAULT_WINDOW_DURATION = 30

@OpenForTesting
internal class EventThrottler(
    private var eventLimit: Int = DEFAULT_EVENT_LIMIT,
    private var windowDuration: Int = DEFAULT_WINDOW_DURATION,
) {
    // Values will be populated by config
    private val eventsLastTimestamp: MutableMap<TrackingEventName, Long> = mutableMapOf()
    private val eventsCount: MutableMap<TrackingEventName, Int> = mutableMapOf()
    private val disabledEvents: MutableSet<TrackingEventName> = mutableSetOf()

    /**
     * @return null if the tracking should not continue,
     * otherwise continue with the returned tracking event.
     */
    @Synchronized
    fun throttle(event: TrackingEvent?): TrackingEvent? {
        if (event == null) return null

        with(event) {
            saveTimestampIfNeverSavedBefore()

            val diff = cachedTimestampDifferenceInSeconds()
            if (diff > windowDuration) updateExpiredTimeWindow()

            if (disabledEvents.contains(name)) return null

            val count = updateTrackLimitCount()
            if (count > eventLimit) return tooManyEvents()
            return event
        }
    }

    private fun TrackingEvent.tooManyEvents(): TrackingEvent =
        InfoEvent(
            TrackingEventName.Misc.TOO_MANY_EVENTS,
            name.value,
        ).also { disabledEvents.add(name) }

    private fun TrackingEvent.updateExpiredTimeWindow() {
        updateTimestamp()
        eventsCount.remove(name)
    }

    private fun TrackingEvent.cachedTimestampDifferenceInSeconds(): Long = (timestamp - cachedEventTimestamp()) / 1000

    private fun TrackingEvent.updateTimestamp() {
        eventsLastTimestamp[name] = timestamp
    }

    private fun TrackingEvent.saveTimestampIfNeverSavedBefore() {
        if (!eventsLastTimestamp.containsKey(name)) {
            eventsLastTimestamp[name] = timestamp
        }
    }

    private fun TrackingEvent.updateTrackLimitCount(): Int {
        val countUpdated = eventCount() + 1
        eventsCount[name] = countUpdated
        return countUpdated
    }

    private fun TrackingEvent.eventCount(): Int = eventsCount[name] ?: 0

    private fun TrackingEvent.cachedEventTimestamp(): Long = eventsLastTimestamp[name] ?: timestamp
}
