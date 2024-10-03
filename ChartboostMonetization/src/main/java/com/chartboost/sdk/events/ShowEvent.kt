package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * ShowEvent
 * An AdEvent subclass passed in show-related callbacks.
 */
class ShowEvent internal constructor(override val adID: String?, override val ad: Ad) : AdEvent
