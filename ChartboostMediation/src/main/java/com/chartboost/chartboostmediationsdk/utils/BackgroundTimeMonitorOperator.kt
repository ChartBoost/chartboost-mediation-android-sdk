/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import androidx.lifecycle.DefaultLifecycleObserver

/**
 * An interface to define the controllable behavior of an object that is responsible for monitoring
 * when the application migrates between the background and foreground.
 **/

/**
 * @suppress
 */
interface BackgroundTimeMonitorOperator : DefaultLifecycleObserver {
    fun backgroundTimeUntilNow(): Long
}
