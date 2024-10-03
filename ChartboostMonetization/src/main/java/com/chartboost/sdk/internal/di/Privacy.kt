package com.chartboost.sdk.internal.di

import android.preference.PreferenceManager
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.privacy.GPP
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.privacy.PrivacyStandardRepository
import com.chartboost.sdk.privacy.TCFv2
import com.chartboost.sdk.privacy.usecase.GetDataUseConsentUseCase
import com.chartboost.sdk.privacy.usecase.GetPrivacyListAsJsonUseCase
import com.chartboost.sdk.privacy.usecase.GetWhitelistedPrivacyStandardsListUseCase
import com.chartboost.sdk.privacy.usecase.PutDataUseConsentUseCaseImpl
import com.chartboost.sdk.privacy.usecase.RemoveDataUseConsentUseCase

interface PrivacyComponent {
    val privacyApi: PrivacyApi
    val tcfv2: TCFv2
    val gpp: GPP
    val gppSid: String?
}

internal class PrivacyModule(
    androidComponent: AndroidComponent,
    trackerComponent: TrackerComponent,
) : PrivacyComponent {
    override val privacyApi: PrivacyApi by lazy {
        val prefs = androidComponent.sharedPreferences
        val eventTracker = trackerComponent.eventTracker
        val repository = PrivacyStandardRepository(prefs, eventTracker)
        val putDataUseConsentUseCase = PutDataUseConsentUseCaseImpl(repository, eventTracker)
        val getDataUseConsentUseCase = GetDataUseConsentUseCase(repository)
        val removeDataUseConsentUseCase = RemoveDataUseConsentUseCase(repository)
        val getPrivacyListAsJsonUseCase = GetPrivacyListAsJsonUseCase()
        val getWhitelistedPrivacyStandardsListUseCase =
            GetWhitelistedPrivacyStandardsListUseCase(repository)
        val api =
            PrivacyApi(
                putDataUseConsentUseCase,
                getDataUseConsentUseCase,
                removeDataUseConsentUseCase,
                getPrivacyListAsJsonUseCase,
                getWhitelistedPrivacyStandardsListUseCase,
                tcfv2,
                gpp,
                gppSid,
            )
        api.setPrivacyConfig(SdkConfiguration.PrivacyStandardsConfig())
        api
    }

    override val tcfv2: TCFv2 by lazy {
        TCFv2(PreferenceManager.getDefaultSharedPreferences(androidComponent.context))
    }

    override val gpp: GPP by lazy {
        GPP(PreferenceManager.getDefaultSharedPreferences(androidComponent.context))
    }
    override val gppSid: String? by lazy {
        gpp.getGppSid()
    }
}
