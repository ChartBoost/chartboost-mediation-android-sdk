/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.json.JsonObject

/**
 * @suppress
 */
data class CachedAd(
    internal val bids: Bids,
) {
    var loadId: String = ""
    var customData: String = ""
    var partnerAd: PartnerAd? = null
    var winningBidInfo: Map<String, String> = mapOf()
    var ilrdJson: JsonObject? = null

    val auctionId: String = bids.auctionId
}
