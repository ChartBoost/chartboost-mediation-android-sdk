package com.chartboost.sdk.privacy.usecase

import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.privacy.PrivacyStandardRepository
import com.chartboost.sdk.privacy.model.CCPA
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.Custom
import com.chartboost.sdk.privacy.model.DataUseConsent
import com.chartboost.sdk.privacy.model.GDPR
import com.chartboost.sdk.privacy.model.LGPD
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName

interface PutDataUseConsentUseCase {
    fun execute(dataUseConsent: DataUseConsent?)
}

internal class PutDataUseConsentUseCaseImpl(
    private val repository: PrivacyStandardRepository,
    eventTracker: EventTrackerExtensions,
) : PutDataUseConsentUseCase, EventTrackerExtensions by eventTracker {
    override fun execute(dataUseConsent: DataUseConsent?) {
        if (dataUseConsent?.privacyStandard?.isEmpty() != false) {
            try {
                CriticalEvent(
                    TrackingEventName.Consent.PERSISTENCE_ERROR,
                    "",
                    "",
                    "",
                ).track()
            } catch (e: Exception) {
                // Adding consent can be done before SDK init but tracking works only with SDK initialised.
            }
            Logger.e("addDataUseConsent failed")
            return
        }

        if (dataUseConsent is GDPR ||
            dataUseConsent is CCPA ||
            dataUseConsent is COPPA ||
            dataUseConsent is LGPD ||
            dataUseConsent is Custom
        ) {
            repository.put(dataUseConsent)
        } else {
            try {
                ErrorEvent(
                    TrackingEventName.Consent.SUBCLASSING_ERROR,
                    dataUseConsent.javaClass.name,
                    "",
                    "",
                ).track()
            } catch (e: Exception) {
                // Adding consent can be done before SDK init but tracking works only with SDK initialised.
            }
            Logger.w(
                "Attempt to addDataUseConsent. Context and DataUseConsent cannot be null.",
            )
        }
    }
}
