/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import kotlinx.serialization.json.jsonObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * @suppress
 */
class Bids(
    val adLoadParams: AdLoadParams,
    val bidsResponse: BidsResponse,
) : Iterable<Bid> {

    private var activeBidIndex = AtomicInteger(0)

    val bids: List<Bid> = bidsResponse.seatbid.map {
        Bid(
            adLoadParams.bannerSize,
            adLoadParams.adIdentifier,
            it,
            adLoadParams.loadId
        )
    }

    val adIdentifier
        get() = adLoadParams.adIdentifier
    val auctionId = bidsResponse.id

    val partnerId: String
        get() = bids.getOrNull(activeBidIndex.get())?.partnerName ?: ""

    val bidInfo: HashMap<String, String>
        get() = (bids.getOrNull(activeBidIndex.get())?.bidInfo ?: hashMapOf()).apply {
            this[AUCTION_ID_KEY] = auctionId
        }

    val activeBid: Bid?
        get() = bids.getOrNull(activeBidIndex.get())

    val rewardedCallbackData = bidsResponse.bidsExt?.rewardedCallbackData

    init {
        bidsResponse.bidsExt?.ilrd?.let { bidsResponseIlrd ->
            bids.forEach { bid ->
                bid.updateIlrd(bidsResponseIlrd.jsonObject, false)
            }
        }
    }

    fun incrementActiveBid() = activeBidIndex.incrementAndGet()

    override fun iterator() = bids.iterator()

    companion object {
        const val AUCTION_ID_KEY = "auction-id"
    }
}
