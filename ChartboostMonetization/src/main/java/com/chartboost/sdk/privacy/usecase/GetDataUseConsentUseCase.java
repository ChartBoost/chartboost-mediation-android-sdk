package com.chartboost.sdk.privacy.usecase;

import com.chartboost.sdk.privacy.PrivacyStandardRepository;
import com.chartboost.sdk.privacy.model.DataUseConsent;

public class GetDataUseConsentUseCase {

    private final PrivacyStandardRepository mRepository;

    public GetDataUseConsentUseCase(PrivacyStandardRepository repository) {
        mRepository = repository;
    }

    public DataUseConsent execute(String privacyStandard) {
        return mRepository.getMap().get(privacyStandard);
    }
}
