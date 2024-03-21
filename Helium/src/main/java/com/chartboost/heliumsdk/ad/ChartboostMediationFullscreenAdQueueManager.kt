/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context

/**
 * The Chartboost Mediation Fullscreen Ad Queue Manager creates [ChartboostMediationFullscreenAdQueue] queues per placement.
 */
object ChartboostMediationFullscreenAdQueueManager {
    /**
     * A map of [ChartboostMediationFullscreenAdQueue]'s keyed by placement name.
     */
    private val placementToFullscreenAdQueues = mutableMapOf<String, ChartboostMediationFullscreenAdQueue>()

    /**
     * Let the manager know when the Chartboost Mediation SDK initialized and automatically
     * start the queues that are currently paused.
     *
     * @param isSdkInitialized Whether the SDK is initialized or not.
     */
    internal fun autoStartQueues(isSdkInitialized: Boolean) {
        placementToFullscreenAdQueues.values.filter { it.isRunning }.forEach { queue ->
            queue.notifyQueueToAutoStart.onSdkInitAutoQueue(isSdkInitialized)
        }
    }

    /**
     * Creates a new [ChartboostMediationFullscreenAdQueue] from the given [String] placement. Otherwise, returns the
     * already created queue for the given placement.
     *
     * @param context The current [Context].
     * @param placementName The name of the placement [String] to create an ad queue.
     *
     * @return A [ChartboostMediationFullscreenAdQueue].
     */
    @JvmStatic
    fun queue(
        context: Context,
        placementName: String,
    ): ChartboostMediationFullscreenAdQueue =
        placementToFullscreenAdQueues.getOrPut(placementName) {
            ChartboostMediationFullscreenAdQueue(context, placementName)
        }
}
