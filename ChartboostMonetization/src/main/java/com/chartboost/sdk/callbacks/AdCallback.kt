package com.chartboost.sdk.callbacks

import com.chartboost.sdk.events.*

/**
 * AdCallback
 * The interface which all Chartboost ad callbacks inherit from.
 * Provides methods to receive notifications related to an ad's actions and to control its behavior.
 */
interface AdCallback {
    /**
     * Called after a cache call, either if an ad has been successfully loaded from the Chartboost servers and cached, or if it failed to load.
     * Implement this method to be notified when an ad is ready to be shown after the cache method has been called.
     *
     * A typical implementation would look like this:
     * ```
     * fun onAdLoaded(event: CacheEvent, error: CacheError?) {
     *     if (error != null) {
     *         // Handle error
     *     } else {
     *         // At this point event.ad.isCached will be true, and the ad is ready to be shown.
     *     }
     * }
     * ```
     *
     * @param event A cache event with information related to the cached ad.
     * @param error An error specifying the failure reason, or null if the operation was successful.
     */
    fun onAdLoaded(
        event: CacheEvent,
        error: CacheError?,
    )

    /**
     * Called after a show call, right before an ad is presented.
     * Implement this method to be notified when an ad is about to be presented.
     *
     * A typical implementation would look like this:
     * ```
     * fun onAdRequestedToShow(event: ShowEvent) {
     *     // Pause ongoing processes like video or gameplay.
     * }
     * ```
     *
     * @param event A show event with information related to the ad to be shown.
     */
    fun onAdRequestedToShow(event: ShowEvent)

    /**
     * Called after a show call, either if the ad has been presented and an ad
     * impression logged, or if the operation failed. Implement this method to be notified when the ad
     * presentation process has finished. This method will be called once for each call to
     * show on an interstitial or rewarded ad. In contrast, this may be called multiple times after
     * showing a banner, either if some error occurs after the ad has been successfully shown
     * or as a result of the banner's automatic content refresh.
     *
     * A common practice is to cache an ad here so there's an ad ready for the next time you need to show it.
     * Note that this is not necessary for banners with automaticallyRefreshesContent set to TRUE.
     *
     * ```
     * fun onAdShown(event: ShowEvent, error: ShowError?) {
     *     if (error != null) {
     *         // Handle error, possibly resuming processes paused in willShowAd:
     *     } else {
     *         event.ad.cache()
     *     }
     * }
     * ```
     *
     * @param event A show event with information related to the ad shown.
     * @param error An error specifying the failure reason, or null if the operation was successful.
     */
    fun onAdShown(
        event: ShowEvent,
        error: ShowError?,
    )

    /**
     * Called after an ad has been clicked.
     * Implement this method to be notified when an ad has been clicked.
     * If the click does not result in the opening of a link, an error will be provided explaining
     * why.
     *
     * A typical implementation would look like this:
     * ```
     * fun onAdClicked(event: ClickEvent, error: ClickError?) {
     *     if (error != null) {
     *         // Handle error
     *     } else {
     *         // Maybe pause ongoing processes like video or gameplay when a banner ad is clicked.
     *     }
     * }
     * ```
     *
     * @param event A click event with information related to the ad clicked.
     * @param error An error specifying the failure reason, or null if the operation was successful.
     */
    fun onAdClicked(
        event: ClickEvent,
        error: ClickError?,
    )

    /**
     * Called after an ad has recorded an impression.
     * Implement this method to be notified when an ad has recorded an impression.
     * This method will be called once a valid impression is recorded after showing the ad.
     *
     * @param event An impression event with information related to the visible ad.
     */
    fun onImpressionRecorded(event: ImpressionEvent)
}
