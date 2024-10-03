package com.chartboost.sdk.events

import com.chartboost.sdk.ads.Ad

/**
 * DismissEvent
 * Passed in reward-related callbacks.
 *
 * @param ad
 * @param adID
 * @param reward The earned reward.
 */
class RewardEvent internal constructor(
    override val adID: String?,
    override val ad: Ad,
    val reward: Int,
) : AdEvent
