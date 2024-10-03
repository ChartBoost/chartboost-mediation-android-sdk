package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.privacy.model.DataUseConsent
import org.json.JSONObject

data class PrivacyBodyFields(
    val openRtbConsent: Int? = null,
    val whitelistedPrivacyStandardsList: MutableList<DataUseConsent>? = mutableListOf(),
    val openRtbGdpr: Int? = null,
    val openRtbCoppa: Int? = null,
    val privacyListAsJson: JSONObject? = null,
    val piDataUseConsent: String? = null,
    val tcfString: String? = null,
    val gppString: String? = null,
    val gppSid: String? = null,
)
