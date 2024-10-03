package com.chartboost.sdk.privacy.model

/**
 * LGPD class implements DataUseConsent class which is meant to be used specifically with LGPD law.
 */
class LGPD(allowBehavioralTargeting: Boolean) : GenericDataUseConsent() {
    init {
        this.privacyStandardName = LGPD_STANDARD
        this.consentValue = allowBehavioralTargeting
    }

    override val consent: Boolean
        get() = consentValue as Boolean

    companion object {
        const val LGPD_STANDARD = "lgpd"
    }
}
