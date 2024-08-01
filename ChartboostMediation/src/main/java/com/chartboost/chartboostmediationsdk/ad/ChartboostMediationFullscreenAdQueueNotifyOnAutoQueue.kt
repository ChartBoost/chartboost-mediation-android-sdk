/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

/**
 * An interface to notify the queue manager of the initialization status
 * of the Chartboost Mediation SDK.
 *
 * @suppress
 */
internal interface ChartboostMediationFullscreenAdQueueNotifyOnAutoQueue {
    /**
     * Notifies whether to automatically start queueing if the SDK is initialized or not.
     */
    fun onSdkInitAutoQueue(isSdkInitialized: Boolean)
}
