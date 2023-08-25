/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.domain.Bid
import com.chartboost.heliumsdk.domain.Bids
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 *
 * Empty body here is fine, so null [Bids] object will result in all null values
 */
@Serializable
class AuctionWinnerRequestBody private constructor(
    @SerialName("auction_id")
    val auctionId: String? = null,

    @SerialName("winner")
    val partnerName: String? = null,

    @SerialName("type")
    val type: String? = null,

    @SerialName("line_item_id")
    val lineItemId: String? = null,

    @SerialName("partner_placement")
    val partnerPlacement: String? = null,

    @SerialName("price")
    val price: Double? = null,

    @SerialName("bidders")
    val bidders: List<AuctionWinnerRequestBidder>? = null
) {
    constructor(bids: Bids) : this(
        auctionId = if (bids.activeBid == null) null else bids.auctionId,
        partnerName = bids.activeBid?.partnerName,
        type = when (bids.activeBid?.isMediation) {
            true -> "mediation"
            false -> "bidding"
            else -> null
        },
        lineItemId = bids.activeBid?.lineItemId,
        partnerPlacement = bids.activeBid?.let { bid ->
            bid.partnerPlacementName.takeIf { bid.isMediation }
        },
        price = bids.activeBid?.price,
        bidders = bids.map(::AuctionWinnerRequestBidder)
    )
}

/**
 * @suppress
 */
@Serializable
class AuctionWinnerRequestBidder private constructor(
    @SerialName("seat")
    val seat: String? = null,

    @SerialName("price")
    val price: Double? = null,

    @SerialName("lurl")
    val lurl: String? = null,

    @SerialName("nurl")
    val nurl: String? = null
) {
    constructor(bid: Bid) : this(
        seat = bid.partnerName,
        price = bid.price,
        lurl = bid.lurl.takeIf { it.isNotEmpty() },
        nurl = bid.nurl.takeIf { it.isNotEmpty() }
    )
}
