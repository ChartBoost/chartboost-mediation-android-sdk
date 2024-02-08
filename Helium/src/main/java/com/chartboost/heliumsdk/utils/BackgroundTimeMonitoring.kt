/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

/**
 * An interface to define the means of providing a new instance of a `BackgroundTimeMonitorOperation`.
 */
/**
 * @suppress
 */
interface BackgroundTimeMonitoring {
    fun startMonitoringOperation(): BackgroundTimeMonitorOperator
}
