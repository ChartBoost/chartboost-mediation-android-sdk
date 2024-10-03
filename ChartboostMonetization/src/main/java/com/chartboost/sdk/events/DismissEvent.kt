package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * DismissEvent
 * Passed in dismiss-related callbacks.
 */
class DismissEvent internal constructor(override val adID: String?, override val ad: Ad) : AdEvent
