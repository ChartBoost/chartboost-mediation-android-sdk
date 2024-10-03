package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * ClickEvent
 * An AdEvent subclass passed in click-related callbacks.
 */
class ClickEvent internal constructor(override val adID: String?, override val ad: Ad) : AdEvent
