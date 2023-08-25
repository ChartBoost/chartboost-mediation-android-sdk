package com.chartboost.heliumsdk.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class BannerSizeBody(
    @SerialName("auction_id")
    val auctionId: String,

    @SerialName("line_item_id")
    val lineItemId: String,

    @SerialName("placement_name")
    val placementName: String,

    @SerialName("partner_name")
    val partnerName: String? = null,

    @SerialName("partner_placement")
    val partnerPlacement: String? = null,

    @SerialName("creative_size")
    val creativeSize: AdSize,

    @SerialName("container_size")
    val containerSize: AdSize,

    @SerialName("request_size")
    val requestSize: AdSize,
)

@Serializable
data class AdSize(
    @SerialName("w")
    val width: Int,

    @SerialName("h")
    val height: Int,
)
