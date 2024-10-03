package com.chartboost.sdk.ads

/**
 * Ad
 * The interface to which all Chartboost ads conform.
 * Provides basic functionalities common to all ads.
 */
sealed interface Ad {
    /**
     * Chartboost location for the ad.
     * Used to obtain ads with increased performance.
     */
    val location: String

    /**
     * Caches an ad.
     * This method will first check if there is a cached ad and, if found, will do nothing.
     * If no cached ad exists, the method will attempt to fetch it from the Chartboost server.
     * Implement `didCacheAd(event: CacheEvent, error: CacheError?)` to be notified of the cache request result.
     */
    fun cache()

    /**
     * Caches an ad with bid response.
     * This method will parse the provided bid response and cache the ad accordingly.
     */
    fun cache(bidResponse: String?)

    /**
     * Shows an ad.
     * This method will first check if there is a cached ad; if found, it will present it.
     */
    fun show()

    /**
     * Clears the ad cache.
     * This will do nothing if there's no cached ad. Otherwise, it will remove any data
     * related to the ad, bringing the ad instance back to a non-cached state. After calling this
     * method, you may call `cache` again and a new ad will be fetched.
     */
    fun clearCache()

    /**
     * Determines if a cached ad exists.
     * @return True if there is a cached ad, and False if not.
     */
    fun isCached(): Boolean
}
