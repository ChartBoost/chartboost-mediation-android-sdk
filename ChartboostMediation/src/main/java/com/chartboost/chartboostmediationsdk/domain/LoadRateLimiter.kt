/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * @suppress
 *
 * This class keeps track of rate limiting on a Chartboost Mediation placement level.
 */
class LoadRateLimiter {
    /**
     * Map of Chartboost Mediation placement to how many seconds to back off for the next load.
     */
    private val loadRateLimitSecondsMap: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

    /**
     * Map of Chartboost Mediation placement to the [SystemClock.uptimeMillis] timestamp that a load is
     * allowed to occur.
     */
    private val uptimeMillisWhenNextLoadIsAllowedMap: ConcurrentHashMap<String, Long> =
        ConcurrentHashMap()

    /**
     * Milliseconds until the next load is allowed. This is 0 if a load is allowed to happen.
     *
     * @param placement For the given Chartboost Mediation placement.
     * @return Milliseconds until the next load is allowed as a [Long].
     */
    fun millisUntilNextLoadIsAllowed(placement: String): Long {
        val uptimeMillisWhenNextLoadIsAllowed = uptimeMillisWhenNextLoadIsAllowedMap[placement] ?: 0
        val timeUntilNextLoad = uptimeMillisWhenNextLoadIsAllowed - SystemClock.uptimeMillis()
        return if (timeUntilNextLoad <= 0) {
            0
        } else {
            timeUntilNextLoad
        }
    }

    /**
     * Gets the previously set load rate limit given to us by the server for a particular Chartboost
     * Mediation placement in seconds. The default is 0.
     *
     * @param placement For the given Chartboost Mediation placement.
     * @return Number of seconds the server told Chartboost Mediation to rate limit this placement as an [Int].
     */
    fun getLoadRateLimitSeconds(placement: String): Int = loadRateLimitSecondsMap[placement] ?: 0

    /**
     * Sets the load rate limit for a Chartboost Mediation placement. Also refreshes
     * [uptimeMillisWhenNextLoadIsAllowedMap] with the next time a load is allowed to happen.
     *
     * @param placement For the given Chartboost Mediation placement.
     * @param durationSeconds How long to rate limit this placement for.
     */
    fun setLoadRateLimit(
        placement: String,
        durationSeconds: Int,
    ) {
        loadRateLimitSecondsMap[placement] = durationSeconds
        uptimeMillisWhenNextLoadIsAllowedMap[placement] =
            SystemClock.uptimeMillis() + durationSeconds * 1000
    }
}
