package com.chartboost.sdk.callbacks

import com.chartboost.sdk.events.DismissEvent

/**
 * DismissibleAdCallback for ads that can be dismissed.
 * Provides methods to receive notifications related to an ad's actions and to control its behavior.
 */
interface DismissibleAdCallback : AdCallback {
    /**
     * Called after an ad is dismissed.
     * Implement this method to be notified when an ad is no longer displayed.
     * Note that this method won't be called for ads that failed to be shown.
     * To handle that case, implement `didShowAd.error`.
     * You may use the error property inside the event to determine if the dismissal
     * was expected or caused by an error.
     *
     * A typical implementation would look like this:
     * ```
     * fun onAdDismiss(event: DismissEvent) {
     *     // Resume processes paused in willShowAd
     * }
     * ```
     *
     * @param event A dismiss event with information related to the dismissed ad.
     */
    fun onAdDismiss(event: DismissEvent)
}
