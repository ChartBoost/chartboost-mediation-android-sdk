/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.domain.Ad.AdType
import com.chartboost.chartboostmediationsdk.domain.AdIdentifier
import com.chartboost.core.ChartboostCore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
class BidRequestImpression private constructor(
    /**
     * Name of our Chartboost Mediation SDK.
     */
    @SerialName("displaymanager")
    private val displayManager: String = "Helium",
    /**
     * The SDK version of our Chartboost Mediation SDK.
     */
    @SerialName("displaymanagerver")
    private val displayManagerVersion: String = ChartboostMediationSdk.getVersion(),
    /**
     * Indicates whether this is a fullscreen or interstitial (1) or a banner (0).
     */
    @SerialName("instl")
    private val fullscreen: Int,
    /**
     * The Chartboost Mediation placement.
     */
    @SerialName("tagid")
    private val tagId: String,
    /**
     * Always use HTTPS
     */
    @SerialName("secure")
    private val secure: Int = 1,
    @SerialName("video")
    private val video: BidRequestImpressionVideo,
    @SerialName("banner")
    private val banner: BidRequestImpressionBanner,
) {
    constructor(
        adIdentifier: AdIdentifier,
        size: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
    ) : this(
        tagId = adIdentifier.placement,
        fullscreen =
            if (adIdentifier.adType == AdType.BANNER ||
                adIdentifier.adType == AdType.ADAPTIVE_BANNER
            ) {
                0
            } else {
                1
            },
        video = BidRequestImpressionVideo(size, adIdentifier.adType),
        banner = BidRequestImpressionBanner(size, adIdentifier.adType),
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestImpressionVideo private constructor(
    /**
     * Mime types use ("video/mp4")
     */
    @SerialName("mimes")
    private val mimes: List<String> = listOf("video/mp4"),
    /**
     * The width dimension.
     */
    @SerialName("w")
    private val width: Int,
    /**
     * The height dimension.
     */
    @SerialName("h")
    private val height: Int,
    /**
     * 2 = In-Banner
     * 5 = Interstitial/modal
     * https://www.iab.com/wp-content/uploads/2016/03/OpenRTB-API-Specification-Version-2-5-FINAL.pdf
     */
    @SerialName("placement")
    private val placement: Int,
    /**
     * Position on screen: 7 (Fullscreen)
     */
    @SerialName("pos")
    private val position: Int,
    /**
     * Use (1,2) 1=Static, 2=HTMLResource
     */
    @SerialName("companiontype")
    private val companionType: List<Int>,
    @SerialName("ext")
    private val ext: BidRequestImpressionExt,
) {
    constructor(
        size: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
        @AdType adType: Int,
    ) : this(
        width = size?.width ?: ChartboostCore.analyticsEnvironment.screenWidthPixels ?: 0,
        height = size?.height ?: ChartboostCore.analyticsEnvironment.screenHeightPixels ?: 0,
        placement = if (adType == AdType.BANNER || adType == AdType.ADAPTIVE_BANNER) 2 else 5,
        position = if (adType == AdType.BANNER || adType == AdType.ADAPTIVE_BANNER) 1 else 7,
        companionType = listOf(1, 2),
        ext = BidRequestImpressionExt(adType),
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestImpressionBanner private constructor(
    /**
     * The width dimension.
     */
    @SerialName("w")
    private val width: Int,
    /**
     * The height dimension.
     */
    @SerialName("h")
    private val height: Int,
    /**
     * Position on screen: 7 (Fullscreen)
     */
    @SerialName("pos")
    private val position: Int,
    /**
     * Indicates if the banner is in the top frame as opposed to an iframe, where 0 = no, 1 = yes.
     */
    @SerialName("topframe")
    private val topFrame: Int = 1,
    @SerialName("ext")
    private val ext: BidRequestImpressionExt,
) {
    constructor(
        size: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
        @AdType adType: Int,
    ) : this(
        width = size?.width ?: ChartboostCore.analyticsEnvironment.screenWidthPixels ?: 0,
        height = size?.height ?: ChartboostCore.analyticsEnvironment.screenHeightPixels ?: 0,
        position = if (adType == AdType.BANNER || adType == AdType.ADAPTIVE_BANNER) 1 else 7,
        ext = BidRequestImpressionExt(adType),
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestImpressionExt private constructor(
    /**
     * The placement type.
     */
    @SerialName("placementtype")
    private val placementType: String,
) {
    constructor(
        @AdType adType: Int,
    ) : this(
        placementType =
            when (adType) {
                AdType.REWARDED_INTERSTITIAL -> "rewarded_interstitial"
                AdType.INTERSTITIAL -> "interstitial"
                AdType.REWARDED -> "rewarded"
                AdType.BANNER -> "banner"
                AdType.ADAPTIVE_BANNER -> "adaptive_banner"
                else -> "unknown"
            },
    )
}
