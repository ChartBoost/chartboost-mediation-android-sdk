package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * CacheEvent
 * An AdEvent subclass passed in cache-related callbacks.
 */
class CacheEvent internal constructor(override val adID: String?, override val ad: Ad) : AdEvent
