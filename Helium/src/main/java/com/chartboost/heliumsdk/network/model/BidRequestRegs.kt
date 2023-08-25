/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.controllers.PrivacyController.PrivacySetting.FALSE
import com.chartboost.heliumsdk.controllers.PrivacyController.PrivacySetting.TRUE
import com.chartboost.heliumsdk.controllers.PrivacyController.PrivacyString.DENIED
import com.chartboost.heliumsdk.controllers.PrivacyController.PrivacyString.GRANTED
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
    val ext: BidRequestRegsExt
) {
    constructor(
        isCoppa: Boolean?,
        gdpr: Int,
        ccpaConsent: Boolean?
    ) : this(
        coppa = if (isCoppa == true) 1 else 0,
        ext = BidRequestRegsExt(gdpr = gdpr, ccpaConsent = ccpaConsent)
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestRegsExt private constructor(
    /**
     * Flag to determine whether or not GDPR applies to this user
     */
    @SerialName("gdpr")
    val gdpr: Int,

    /**
     * Flag to determine whether or not CCPA applies to this user
     */
    @SerialName("us_privacy")
    val usPrivacy: String?
) {
    constructor(
        gdpr: Int,
        ccpaConsent: Boolean?
    ) : this(
        gdpr = if (gdpr == TRUE.value) TRUE.value else FALSE.value,
        usPrivacy = when (ccpaConsent) {
            true -> GRANTED.consentString
            false -> DENIED.consentString
            null -> null
        }
    )
}
