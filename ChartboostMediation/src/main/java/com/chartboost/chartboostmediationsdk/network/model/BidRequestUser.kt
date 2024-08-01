/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.domain.Keywords
import com.chartboost.core.ChartboostCore
import com.chartboost.core.consent.ConsentKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
class BidRequestUser private constructor(
    @SerialName("ext")
    val bidRequestUserExt: BidRequestUserExt,
    @SerialName("consent")
    val tcConsentString: String? = ChartboostCore.consent.consents[ConsentKeys.TCF]?.takeIf { it.isNotBlank() },
) {
    constructor(
        consent: Boolean?,
        impressionDepth: Int,
        keywords: Keywords,
    ) : this(
        bidRequestUserExt =
            BidRequestUserExt(
                consentGiven = consent,
                impressionDepth = impressionDepth,
                keywords = keywords,
            ),
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestUserExt private constructor(
    /**
     * The consent status. Only included in the payload when '1'
     */
    @SerialName("consent")
    val consent: String? = null,
    /**
     * The impression depth
     */
    @SerialName("impdepth")
    val impressionDepth: Int,
    /**
     * The duration of the session
     */
    @SerialName("sessionduration")
    val sessionDuration: Int = ChartboostCore.analyticsEnvironment.appSessionDurationSeconds.toInt(),
    /**
     * The publisher provided user_id
     */
    @SerialName("publisher_user_id")
    val publisherUserId: String? = ChartboostCore.analyticsEnvironment.playerIdentifier?.takeIf { it.isNotEmpty() },
    /**
     * [Keywords] representing key-value pairs for Chartboost Mediation's open RTB requests
     */
    @SerialName("keywords")
    val keywords: Keywords,
) {
    constructor(
        consentGiven: Boolean?,
        impressionDepth: Int,
        keywords: Keywords,
    ) : this(
        consent = if (consentGiven == true) "1" else null,
        impressionDepth = impressionDepth,
        // this is necessary to make sure we do not include { "keywords": null } in the bid request
        keywords = keywords,
    )
}
