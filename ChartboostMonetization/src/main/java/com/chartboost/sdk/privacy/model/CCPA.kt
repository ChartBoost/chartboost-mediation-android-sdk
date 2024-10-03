package com.chartboost.sdk.privacy.model

/**
 * CCPA class implements DataUseConsent class which is meant to be used specifically with CCPA law.
 * Use one of the possible consents provided via CCPA_CONSENT interface to apply correct consent.
 */
class CCPA(consent: CCPA_CONSENT) : GenericDataUseConsent() {
    /**
     * Use interface CCPA_CONSENT
     */
    init {
        if (isValidConsent(consent.value)) {
            this.privacyStandardName = CCPA_STANDARD
            this.consentValue = consent.value
        } else {
            handleException("Invalid CCPA consent values. Use provided values or Custom class. Value: $consent")
        }
    }

    override val consent: String
        get() = consentValue as String

    /**
     * CCPA compliance settings:
     * OPT_IN_SALE(1YN-) means the user consents (Behavioral and Contextual Ads)
     * OPT_OUT_SALE(1YY-) means the user does not consent to targeting (Contextual ads)
     */
    enum class CCPA_CONSENT(val value: String) {
        OPT_OUT_SALE("1YY-"),
        OPT_IN_SALE("1YN-"),
        ;

        companion object {
            @JvmStatic
            fun fromValue(value: String): CCPA_CONSENT? {
                if (OPT_OUT_SALE.value == value) {
                    return OPT_OUT_SALE
                } else if (OPT_IN_SALE.value == value) {
                    return OPT_IN_SALE
                }
                return null
            }
        }
    }

    private fun isValidConsent(consent: String): Boolean {
        return CCPA_CONSENT.OPT_OUT_SALE.value == consent || CCPA_CONSENT.OPT_IN_SALE.value == consent
    }

    companion object {
        const val CCPA_STANDARD = "us_privacy"
    }
}
