package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * ImpressionEvent
 * A AdEvent subclass passed in impression-related callback functions.
 */
class ImpressionEvent internal constructor(override val adID: String?, override val ad: Ad) : AdEvent
