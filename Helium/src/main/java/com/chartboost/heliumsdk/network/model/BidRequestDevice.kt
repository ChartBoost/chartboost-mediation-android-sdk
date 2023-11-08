/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.ad.HeliumBannerAd
import com.chartboost.heliumsdk.utils.Environment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
class BidRequestDevice private constructor(
    /**
     * The device's user agent.
     */
    @SerialName("ua")
    val userAgent: String = Environment.userAgent,

    /**
     * The device's limit ad tracking.
     */
    @SerialName("lmt")
    val lmt: Int? = Environment.lmt,

    /**
     * The device's type.
     */
    @SerialName("devicetype")
    val deviceType: Int? = Environment.deviceType,

    /**
     * The device's make.
     */
    @SerialName("make")
    val make: String = Environment.manufacturer,

    /**
     * The device's model.
     */
    @SerialName("model")
    val model: String = Environment.model,

    /**
     * The operating system.
     */
    @SerialName("os")
    val operatingSystem: String = Environment.operatingSystem,

    /**
     * The operating system version.
     */
    @SerialName("osv")
    val operatingSystemVersion: String = Environment.operatingSystemVersion,

    /**
     * The height dimension.
     */
    @SerialName("h")
    val height: Int,

    /**
     * The width dimension.
     */
    @SerialName("w")
    val width: Int,

    /**
     * Device's screen density
     */
    @SerialName("pxratio")
    val pxRatio: Float? = Environment.pxRatio,

    /**
     * The local language of the device where this bid is being made.
     */
    @SerialName("language")
    val language: String? = Environment.language,

    /**
     * The carrier from the device where this bid is being made.
     */
    @SerialName("carrier")
    val carrier: String? = Environment.carrierName,

    /**
     * The connection type of this bid.
     */
    @SerialName("connectiontype")
    val connectionType: Int = Environment.connectionType,

    /**
     * The device's ifa.
     */
    @SerialName("ifa")
    val ifa: String? = Environment.ifa,

    @SerialName("geo")
    val geo: BidRequestDeviceGeo = BidRequestDeviceGeo(),

    @SerialName("ext")
    val ext: BidRequestDeviceExt = BidRequestDeviceExt()
) {
    constructor(
        size: HeliumBannerAd.HeliumBannerSize?,
    ) : this(
        height = size?.height ?: Environment.displayHeight,
        width = size?.width ?: Environment.displayWidth,
        ifa = Environment.ifa
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestDeviceGeo(
    /**
     * Set the UTC offset in minutes.
     */
    @SerialName("utcoffset")
    private val utcOffset: Int = Environment.utcOffsetTime
)

/**
 * @suppress
 */
@Serializable
class BidRequestDeviceExt private constructor(
    /**
     * This bid is from the following Mobile Country Code and Mobile Network Code.
     */
    @SerialName("mccmnc")
    private val mCCMNC: String? = Environment.mccmnc,

    /**
     * The app set ID.
     */
    @SerialName("ifv")
    private val ifv: String? = null,

    /**
     * The app set ID scope.
     */
    @SerialName("appsetidscope")
    private val appsetidscope: Int? = Environment.appSetIdScope
) {
    constructor() : this(
        ifv = Environment.appSetId,
        appsetidscope = Environment.appSetIdScope
    )
}
