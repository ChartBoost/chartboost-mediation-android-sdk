/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */
package com.chartboost.chartboostmediationsdk.utils

import androidx.lifecycle.LifecycleOwner
import java.util.Date

internal class BackgroundTimeMonitorOperation : BackgroundTimeMonitorOperator {
    private var lastBackgroundedDate: Date? = null
    private var totalBackgroundedMs: Long = 0

    init {
        if (LifecycleStatusObserver.status == LifecycleStatus.BACKGROUND) {
            lastBackgroundedDate = Date()
        }
    }

    /**
     * Calculate and return the total time spent in the background during the lifetime
     * of an instance of this class.
     */
    override fun backgroundTimeUntilNow(): Long {
        // If in the background right now while still monitoring, need to add in that time.
        commitLastBackgroundedDate()

        return totalBackgroundedMs
    }

    /**
     * Respond to `onStart`, signifying that the application has entered the foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        commitLastBackgroundedDate()
    }

    /**
     * Respond to `onStop`, signifying that the application has entered the background.
     */
    override fun onStop(owner: LifecycleOwner) {
        lastBackgroundedDate = Date()
    }

    private fun commitLastBackgroundedDate() {
        val lastBackgroundedDate = lastBackgroundedDate ?: return
        totalBackgroundedMs += Date().time - lastBackgroundedDate.time
        this.lastBackgroundedDate = null
    }
}
