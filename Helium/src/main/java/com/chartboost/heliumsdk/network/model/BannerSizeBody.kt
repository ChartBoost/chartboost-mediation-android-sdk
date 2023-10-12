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

    @SerialName("creative_size")
    val creativeSize: BannerAdDimensions,

    @SerialName("container_size")
    val containerSize: BannerAdDimensions,

    @SerialName("request_size")
    val requestSize: BannerAdDimensions,
)
