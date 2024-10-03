package com.chartboost.sdk.privacy.usecase;

import com.chartboost.sdk.privacy.PrivacyStandardRepository;

public class RemoveDataUseConsentUseCase {

    private final PrivacyStandardRepository mRepository;

    public RemoveDataUseConsentUseCase(PrivacyStandardRepository repository) {
        mRepository = repository;
    }

    public void execute(String privacyStandard) {
        mRepository.remove(privacyStandard);
    }

}
