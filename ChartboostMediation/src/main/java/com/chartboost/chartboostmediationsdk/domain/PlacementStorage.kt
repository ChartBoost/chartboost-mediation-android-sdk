/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * @suppress
 */
object PlacementStorage {
    private const val REFRESH_BY_DEFAULT = true
    private const val DEFAULT_REFRESH_TIME_SECONDS = 30
    private const val MINIMUM_REFRESH_TIME_SECONDS = 10
    private const val MAXIMUM_REFRESH_TIME_SECONDS = 240
    private const val MAXIMUM_FAILURES_BEFORE_PENALTY_REFRESH_TIME = 3
    private const val MINIMUM_QUEUE_SIZE = 1

    private val defaultQueueSize = AppConfigStorage.defaultQueueSize
    private val refreshTimes = HashMap<String, Int>().withDefault { DEFAULT_REFRESH_TIME_SECONDS }
    private val placementQueueSize = HashMap<String, Int>().withDefault { defaultQueueSize }

    fun shouldRefresh(placement: String): Boolean {
        if (!refreshTimes.containsKey(placement)) {
            return REFRESH_BY_DEFAULT
        } else if (refreshTimes.getValue(placement) <= 0) {
            return false
        }
        return true
    }

    fun addRefreshTime(
        placement: String,
        refreshTime: Int,
    ) {
        when (refreshTime) {
            0 -> {
                refreshTimes[placement] = 0
            }

            else -> {
                when {
                    refreshTime < MINIMUM_REFRESH_TIME_SECONDS -> {
                        refreshTimes[placement] = MINIMUM_REFRESH_TIME_SECONDS
                    }

                    refreshTime > MAXIMUM_REFRESH_TIME_SECONDS -> {
                        refreshTimes[placement] = MAXIMUM_REFRESH_TIME_SECONDS
                    }

                    else -> {
                        refreshTimes[placement] = refreshTime
                    }
                }
            }
        }
    }

    fun getRefreshTime(placement: String): Int {
        if (!refreshTimes.containsKey(placement)) {
            return DEFAULT_REFRESH_TIME_SECONDS
        }
        return refreshTimes.getValue(placement)
    }

    fun getMaxRefreshTime(): Int = MAXIMUM_REFRESH_TIME_SECONDS

    fun getMaxTriesUntilPenaltyTime(): Int = MAXIMUM_FAILURES_BEFORE_PENALTY_REFRESH_TIME

    fun addQueueSize(
        placement: String,
        queueSize: Int,
        maxQueueSize: Int,
    ) {
        val safeMaxQueueSize = maxQueueSize.coerceAtLeast(MINIMUM_QUEUE_SIZE)
        placementQueueSize[placement] = queueSize.coerceIn(MINIMUM_QUEUE_SIZE, safeMaxQueueSize)
    }

    fun getQueueSize(placement: String): Int {
        if (!placementQueueSize.containsKey(placement)) {
            return MINIMUM_QUEUE_SIZE
        }
        return placementQueueSize.getValue(placement)
    }
}
