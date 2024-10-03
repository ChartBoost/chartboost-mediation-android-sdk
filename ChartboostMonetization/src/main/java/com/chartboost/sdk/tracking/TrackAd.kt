package com.chartboost.sdk.tracking

internal typealias Pixels = Int

data class TrackAd(
    val location: String = "",
    val adType: String = "",
    val adImpressionId: String? = "",
    val adCreativeId: String = "",
    val adCreativeType: String = "",
    val adMarkup: String = "",
    val templateUrl: String = "",
    val adSize: AdSize? = null,
) {
    private val shortImpressionId: String?
        get() = adImpressionId?.substring(0, adImpressionId.length.coerceAtMost(20))

    override fun toString(): String {
        return "TrackAd:" +
            " location: $location" +
            " adType: $adType" +
            " adImpressionId: $shortImpressionId" +
            " adCreativeId: $adCreativeId" +
            " adCreativeType: $adCreativeType" +
            " adMarkup: $adMarkup" +
            " templateUrl: $templateUrl"
    }

    data class AdSize(
        val height: Pixels,
        val width: Pixels,
    )
}
