/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * @suppress
 *
 * This class keeps track of rate limiting on a Helium placement level.
 */
class LoadRateLimiter {
    /**
     * Map of Helium placement to how many seconds to back off for the next load.
     */
    private val loadRateLimitSecondsMap: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

    /**
     * Map of Helium placement to the [SystemClock.uptimeMillis] timestamp that a load is
     * allowed to occur.
     */
    private val uptimeMillisWhenNextLoadIsAllowedMap: ConcurrentHashMap<String, Long> =
        ConcurrentHashMap()

    /**
     * Milliseconds until the next load is allowed. This is 0 if a load is allowed to happen.
     *
     * @param placement For the given Helium placement.
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
     * Gets the previously set load rate limit given to us by the server for a particular Helium
     * placement in seconds. The default is 0.
     *
     * @param placement For the given Helium placement.
     * @return Number of seconds the server told Helium to rate limit this placement as an [Int].
     */
    fun getLoadRateLimitSeconds(placement: String): Int {
        return loadRateLimitSecondsMap[placement] ?: 0
    }

    /**
     * Sets the load rate limit for a Helium placement. Also refreshes
     * [uptimeMillisWhenNextLoadIsAllowedMap] with the next time a load is allowed to happen.
     *
     * @param placement For the given Helium placement.
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
