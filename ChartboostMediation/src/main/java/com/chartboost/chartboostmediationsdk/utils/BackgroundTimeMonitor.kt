/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

/**
 * An implementation of `BackgroundTimeMonitoring`
 */
internal class BackgroundTimeMonitor : BackgroundTimeMonitoring {
    override fun startMonitoringOperation(): BackgroundTimeMonitorOperator = BackgroundTimeMonitorOperation()
}
