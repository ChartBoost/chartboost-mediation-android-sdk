/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers.banners

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.chartboost.heliumsdk.utils.LogController

/**
 * @suppress
 */
fun interface AdRefresherCallback {
    fun onAdNeedsRefreshing()
}

private const val SECOND_IN_MILLISECONDS = 1000L

/**
 * @suppress
 */
class AdRefresher(
    private val initialRefreshRateSec: Int,
    private val penaltyRefreshRateSec: Int,
    private val maxFailuresUntilPenaltyTime: Int,
    private val adRefresherCallback: AdRefresherCallback
) {

    private var isRefreshing = false
    private var refreshesFailed = 0
    private var totalTimePausedMs: Long = 0
    private var timeStartedRefreshingMs: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable {
        LogController.i("Helium AdRefresherCallback onAdNeedsRefreshing.")
        adRefresherCallback.onAdNeedsRefreshing()
    }
    private var isResumed = false

    private fun scheduleNextRefresh() {
        // If the number of failures exceeds the max allowed failures, go into penalty refresh rate.
        // We also subtract the total time already spent looking at the ad.
        var delayTimeMs = getRefreshTimeInSeconds() * SECOND_IN_MILLISECONDS
        if (totalTimePausedMs > delayTimeMs) {
            delayTimeMs = 0
        } else {
            delayTimeMs -= totalTimePausedMs
        }
        // To enable debug logging set HeliumSdk.setDebugMode(true)
        LogController.i(
            "Helium AdRefresherCallback start. Current refresh rate at: " +
                    "${if (refreshesFailed < maxFailuresUntilPenaltyTime) initialRefreshRateSec else penaltyRefreshRateSec}s. " +
                    "Time to the next update: ${delayTimeMs}ms"
        )
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, delayTimeMs)
        timeStartedRefreshingMs = SystemClock.uptimeMillis()
    }

    private fun getRefreshTimeInSeconds() =
        if (refreshesFailed < maxFailuresUntilPenaltyTime) {
            initialRefreshRateSec
        } else {
            penaltyRefreshRateSec
        }

    fun start() {
        isRefreshing = true
        totalTimePausedMs = 0
        if (isResumed) {
            resume()
            isResumed = false
        }
    }

    fun resume() {
        isResumed = true
        if (isRefreshing) {
            scheduleNextRefresh()
        }
    }

    fun markLoadFailed() {
        if (isRefreshing) {
            refreshesFailed++
            totalTimePausedMs = 0
            scheduleNextRefresh()
        }
    }

    fun markLoadSuccess() {
        if (isRefreshing) {
            refreshesFailed = 0
            totalTimePausedMs = 0
            scheduleNextRefresh()
        }
    }

    fun cancel(resetTimer: Boolean = true) {
        LogController.i("Helium AdRefresherCallback cancel.")
        isResumed = false
        handler.removeCallbacks(refreshRunnable)
        if (resetTimer) {
            totalTimePausedMs = 0
            isRefreshing = false
        } else {
            totalTimePausedMs += SystemClock.uptimeMillis() - timeStartedRefreshingMs
        }
        LogController.i("Helium AdRefresherCallback reset timer. Viewed for: $totalTimePausedMs")
    }
}
