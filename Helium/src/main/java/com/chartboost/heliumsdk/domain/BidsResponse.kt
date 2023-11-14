/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * @suppress
 */
@Serializable
data class BidsResponse(
    @SerialName(ID_KEY)
    val id: String,

    @SerialName(SEAT_BID_KEY)
    val seatbid: List<BidResponse>,

    @SerialName(EXT_KEY)
    val bidsExt: BidsExt? = null
) {
    companion object {
        private const val ID_KEY = "id"
        private const val SEAT_BID_KEY = "seatbid"
        private const val EXT_KEY = "ext"

        val EMPTY_BIDS_RESPONSE = BidsResponse("", emptyList(), null)
    }
}

/**
 * @suppress
 */
@Serializable
data class BidsExt(
    @SerialName(RESPONSE_TIME_MILLIS_KEY)
    val responseTimeMillis: JsonElement?,

    @SerialName(ILRD_KEY)
    val ilrd: JsonElement? = null,

    @SerialName(ERRORS_KEY)
    val errors: JsonElement? = null,

    @SerialName(REWARDED_CALLBACK_KEY)
    var rewardedCallbackData: RewardedCallbackData? = null
) {
    companion object {
        private const val ILRD_KEY = "ilrd"
        private const val ERRORS_KEY = "errors"
        private const val RESPONSE_TIME_MILLIS_KEY = "responsetimemillis"
        private const val REWARDED_CALLBACK_KEY = "rewarded_callback";
    }
}
