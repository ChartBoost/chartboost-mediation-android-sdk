/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.ad.HeliumBannerAd
import com.chartboost.heliumsdk.domain.Ad.AdType
import com.chartboost.heliumsdk.domain.AdIdentifier
import com.chartboost.heliumsdk.utils.Environment
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
     * The SDK version of our Helium SDK.
     */
    @SerialName("displaymanagerver")
    private val displayManagerVersion: String = HeliumSdk.getVersion(),

    /**
     * Indicates whether this is a fullscreen or interstitial (1) or a banner (0).
     */
    @SerialName("instl")
    private val fullscreen: Int,

    /**
     * The Helium placement.
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
    private val banner: BidRequestImpressionBanner
) {
    constructor(
        adIdentifier: AdIdentifier,
        size: HeliumBannerAd.HeliumBannerSize?
    ) : this(
        tagId = adIdentifier.placementName,
        fullscreen = if (adIdentifier.adType == AdType.BANNER) 0 else 1,
        video = BidRequestImpressionVideo(size, adIdentifier.adType),
        banner = BidRequestImpressionBanner(size, adIdentifier.adType)
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
    private val ext: BidRequestImpressionExt
) {
    constructor(
        size: HeliumBannerAd.HeliumBannerSize?,
        @AdType adType: Int,
    ) : this(
        width = size?.width ?: Environment.displayWidth,
        height = size?.height ?: Environment.displayHeight,
        placement = if (adType == AdType.BANNER) 2 else 5,
        position = if (adType == AdType.BANNER) 1 else 7,
        companionType = listOf(1, 2),
        ext = BidRequestImpressionExt(adType)
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
    private val ext: BidRequestImpressionExt
) {
    constructor(
        size: HeliumBannerAd.HeliumBannerSize?,
        @AdType adType: Int
    ) : this(
        width = size?.width ?: Environment.displayWidth,
        height = size?.height ?: Environment.displayHeight,
        position = if (adType == AdType.BANNER) 1 else 7,
        ext = BidRequestImpressionExt(adType)
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
    private val placementType: String
) {
    constructor(@AdType adType: Int) : this(
        placementType = when (adType) {
            AdType.REWARDED_INTERSTITIAL -> "rewarded_interstitial"
            AdType.INTERSTITIAL -> "interstitial"
            AdType.REWARDED -> "rewarded"
            AdType.BANNER -> "banner"
            else -> "unknown"
        }
    )
}

