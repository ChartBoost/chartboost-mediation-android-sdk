/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A lifecycle observer that observes `onStart` and `onStop` lifecycle events.
 * Upon initialization, the status variable is set to UNKNOWN.  It changes to
 * BACKGROUND and FOREGROUND upon execution of the appropriate event.
 */
internal class LifecycleStatusObserver : DefaultLifecycleObserver {
    companion object {
        var status: LifecycleStatus = LifecycleStatus.UNKNOWN
    }

    /**
     * Respond to `onStart`, signifying that the application has entered the foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        status = LifecycleStatus.FOREGROUND
    }

    /**
     * Respond to `onStop`, signifying that the application has entered the background.
     */
    override fun onStop(owner: LifecycleOwner) {
        status = LifecycleStatus.BACKGROUND
    }
}
