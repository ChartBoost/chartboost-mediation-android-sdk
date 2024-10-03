package com.chartboost.sdk.callbacks

import com.chartboost.sdk.events.StartError

/**
 * Provides a callback to be executed when the SDK finishes initializing.
 */
fun interface StartCallback {
    /**
     * Called when the SDK initialization is complete.
     *
     * @param error An error specifying the failure reason, or null if the initialization was successful.
     */
    fun onStartCompleted(error: StartError?)
}
