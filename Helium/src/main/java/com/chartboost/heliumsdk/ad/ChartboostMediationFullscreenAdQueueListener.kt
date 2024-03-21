/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

/**
 * Listener for fullscreen ad queue events.
 */
interface ChartboostMediationFullscreenAdQueueListener {
    /**
     * Called when a fullscreen ad queue has been updated.
     *
     * @param adQueue The fullscreen ad queue.
     * @param result The ad load result of this event.
     * @param numberOfAdsReady The number of ads that exist in the queue.
     */
    fun onFullScreenAdQueueUpdated(
        adQueue: ChartboostMediationFullscreenAdQueue,
        result: AdLoadResult,
        numberOfAdsReady: Int,
    )

    /**
     * Called when a fullscreen ad queue has removed an expired ad from its queue.
     *
     * @param adQueue The fullscreen ad queue.
     * @param numberOfAdsReady The number of ads that exist in the queue.
     */
    fun onFullscreenAdQueueExpiredAdRemoved(
        adQueue: ChartboostMediationFullscreenAdQueue,
        numberOfAdsReady: Int,
    )
}
