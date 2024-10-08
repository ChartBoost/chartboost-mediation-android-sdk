/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class PartnerImpressionRequestBody(
    @SerialName("auction_id")
    val auctionId: String? = null,
)
