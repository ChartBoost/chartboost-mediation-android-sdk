/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * @suppress
 *
 * Used to keep track of the showing state of fullscreen ads. Allows observers to listen to said
 * events.
 */
class FullscreenAdShowingState {
    /**
     * Observer to track fullscreen ad showing state.
     */
    interface FullscreenAdShowingStateObserver {
        /**
         * Called when any fullscreen ad is shown.
         */
        fun onFullscreenAdShown()

        /**
         * Called when any fullscreen ad is dismissed.
         */
        fun onFullscreenAdDismissed()
    }

    /**
     * Set of observers to send events to.
     */
    private val observers: MutableSet<FullscreenAdShowingStateObserver> = mutableSetOf()

    /**
     * The current fullscreen ad showing state. It is recommended to check this from the main thread.
     */
    var isFullscreenAdShowing = false

    /**
     * Subscribes an observer. This will happen immediately on the main thread and will post to
     * the main thread from any other thread.
     */
    fun subscribe(fullscreenAdShowingStateObserver: FullscreenAdShowingStateObserver) {
        CoroutineScope(Main.immediate).launch {
            observers.add(fullscreenAdShowingStateObserver)
        }
    }

    /**
     * Unsubscribes an observer. This will happen immediately on the main thread and will post to
     * the main thread from any other thread.
     */
    fun unsubscribe(fullscreenAdShowingStateObserver: FullscreenAdShowingStateObserver) {
        CoroutineScope(Main.immediate).launch {
            observers.remove(fullscreenAdShowingStateObserver)
        }
    }

    fun notifyFullscreenAdShown() {
        isFullscreenAdShowing = true
        for (observer in observers) {
            CoroutineScope(Main.immediate).launch {
                observer.onFullscreenAdShown()
            }
        }
    }

    fun notifyFullscreenAdClosed() {
        isFullscreenAdShowing = false
        for (observer in observers) {
            CoroutineScope(Main.immediate).launch {
                observer.onFullscreenAdDismissed()
            }
        }
    }
}
