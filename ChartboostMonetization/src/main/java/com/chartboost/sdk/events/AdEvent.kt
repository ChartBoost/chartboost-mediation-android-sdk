package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * The base class from which all callbacks inherit.
 * Event objects are passed as parameters to all ad callbacks to provide useful context.
 *
 * Param: adID - A string that uniquely identifies the cached ad, updated when a cache operation ends
 * Param: ad -  Ad object contains ads information
 */
sealed interface AdEvent {
    val adID: String?
    val ad: Ad
}
