package com.chartboost.sdk.privacy.model

/**
 * GDPR class implements DataUseConsent class which is meant to be used specifically with GDPR law.
 * Use one of the possible consents provided via GDPR_CONSENT interface to apply correct consent.
 */
class GDPR(consent: GDPR_CONSENT) : GenericDataUseConsent() {
    /**
     * Use interface GDPR_CONSENT
     */
    init {
        if (isValidConsent(consent.value)) {
            this.privacyStandardName = GDPR_STANDARD
            this.consentValue = consent.value
        } else {
            handleException("Invalid GDPR consent values. Use provided values or Custom class. Value: $consent")
        }
    }

    override val consent: String
        get() = consentValue as String

    /**
     * GDPR compliance settings:
     * NON_BEHAVIORAL(0) means the user does not consent to targeting (Contextual ads)
     * BEHAVIORAL(1) means the user consents (Behavioral and Contextual Ads)
     */
    enum class GDPR_CONSENT(val value: String) {
        NON_BEHAVIORAL("0"),
        BEHAVIORAL("1"),
        ;

        companion object {
            @JvmStatic
            fun fromValue(value: String): GDPR_CONSENT? {
                if (NON_BEHAVIORAL.value == value) {
                    return NON_BEHAVIORAL
                } else if (BEHAVIORAL.value == value) {
                    return BEHAVIORAL
                }
                return null
            }
        }
    }

    private fun isValidConsent(consent: String): Boolean {
        return GDPR_CONSENT.NON_BEHAVIORAL.value == consent || GDPR_CONSENT.BEHAVIORAL.value == consent
    }

    companion object {
        const val GDPR_STANDARD = "gdpr"
    }
}
