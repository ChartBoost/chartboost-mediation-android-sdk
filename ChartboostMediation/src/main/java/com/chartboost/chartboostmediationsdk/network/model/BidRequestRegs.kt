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
class BidRequestRegs private constructor(
    /**
     * Flag to determine whether or not COPPA applies to this user
     */
    @SerialName("coppa")
    val coppa: Int,
    @SerialName("ext")
    val ext: BidRequestRegsExt,
) {
    constructor(
        isCoppa: Boolean?,
        gdpr: String?,
        usPrivacy: String?,
    ) : this(
        coppa = if (isCoppa == true) 1 else 0,
        ext =
            BidRequestRegsExt(
                gdpr = if (gdpr == "1") 1 else 0,
                usPrivacy = usPrivacy,
            ),
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestRegsExt(
    /**
     * Flag to determine whether or not GDPR applies to this user
     */
    @SerialName("gdpr")
    val gdpr: Int,
    /**
     * The IAB US Privacy String.
     */
    @SerialName("us_privacy")
    val usPrivacy: String?,
)
