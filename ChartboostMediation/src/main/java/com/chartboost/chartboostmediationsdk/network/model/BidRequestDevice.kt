/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.utils.Environment
import com.chartboost.core.ChartboostCore
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
    val userAgent: String,
    /**
     * The device's limit ad tracking.
     */
    @SerialName("lmt")
    val lmt: Int?,
    /**
     * The device's type.
     */
    @SerialName("devicetype")
    val deviceType: Int?,
    /**
     * The device's make.
     */
    @SerialName("make")
    val make: String,
    /**
     * The device's model.
     */
    @SerialName("model")
    val model: String,
    /**
     * The operating system.
     */
    @SerialName("os")
    val operatingSystem: String,
    /**
     * The operating system version.
     */
    @SerialName("osv")
    val operatingSystemVersion: String,
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
    val pxRatio: Float?,
    /**
     * The local language of the device where this bid is being made.
     */
    @SerialName("language")
    val language: String?,
    /**
     * The carrier from the device where this bid is being made.
     */
    @SerialName("carrier")
    val carrier: String?,
    /**
     * The connection type of this bid.
     */
    @SerialName("connectiontype")
    val connectionType: Int,
    /**
     * The device's ifa.
     */
    @SerialName("ifa")
    val ifa: String?,
    @SerialName("geo")
    val geo: BidRequestDeviceGeo,
    @SerialName("ext")
    val ext: BidRequestDeviceExt,
) {
    companion object {
        suspend operator fun invoke(
            userAgent: String? = null,
            lmt: Int? = null,
            deviceType: Int? = Environment.deviceType,
            make: String = ChartboostCore.analyticsEnvironment.deviceMake,
            model: String = ChartboostCore.analyticsEnvironment.deviceModel,
            operatingSystem: String = ChartboostCore.analyticsEnvironment.osName,
            operatingSystemVersion: String = ChartboostCore.analyticsEnvironment.osVersion,
            height: Int? = null,
            width: Int? = null,
            pxRatio: Float? = ChartboostCore.analyticsEnvironment.screenScale,
            language: String? = Environment.language,
            carrier: String? = Environment.carrierName,
            connectionType: Int = Environment.connectionType,
            ifa: String? = null,
            geo: BidRequestDeviceGeo = BidRequestDeviceGeo(),
            ext: BidRequestDeviceExt? = null,
            size: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
        ): BidRequestDevice =
            BidRequestDevice(
                userAgent = userAgent ?: ChartboostCore.analyticsEnvironment.getUserAgent() ?: "",
                lmt =
                    lmt
                        ?: if (ChartboostCore.analyticsEnvironment.getLimitAdTrackingEnabled() == true) 1 else 0,
                deviceType = deviceType,
                make = make,
                model = model,
                operatingSystem = operatingSystem,
                operatingSystemVersion = operatingSystemVersion,
                height =
                    size?.height ?: height
                        ?: ChartboostCore.analyticsEnvironment.screenHeightPixels ?: 0,
                width =
                    size?.width ?: width
                        ?: ChartboostCore.analyticsEnvironment.screenWidthPixels ?: 0,
                pxRatio = pxRatio,
                language = language,
                carrier = carrier,
                connectionType = connectionType,
                ifa = ifa ?: ChartboostCore.analyticsEnvironment.getAdvertisingIdentifier(),
                geo = geo,
                ext = ext ?: BidRequestDeviceExt(),
            )
    }
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
    private val utcOffset: Int = Environment.utcOffsetTime,
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
    private val appsetidscope: Int? = null,
) {
    companion object {
        suspend operator fun invoke(
            mCCMNC: String? = null,
            ifv: String? = null,
            appsetidscope: Int? = null,
        ) = BidRequestDeviceExt(
            mCCMNC = mCCMNC ?: Environment.mccmnc,
            ifv = ifv ?: ChartboostCore.analyticsEnvironment.getVendorIdentifier(),
            appsetidscope =
                appsetidscope
                    ?: ChartboostCore.analyticsEnvironment.getVendorIdentifierScope().ordinal,
        )
    }
}
