package com.chartboost.sdk.privacy.model

/**
 * COPPA class implements DataUseConsent class which is meant to be used specifically with COPPA law.
 * Use one of the possible consents provided via COPPA_CONSENT interface to apply correct consent.
 */
class COPPA(isChildDirected: Boolean) : GenericDataUseConsent() {
    init {
        this.privacyStandardName = COPPA_STANDARD
        this.consentValue = isChildDirected
    }

    override val consent: Boolean
        get() = consentValue as Boolean

    companion object {
        const val COPPA_STANDARD = "coppa"
    }
}
