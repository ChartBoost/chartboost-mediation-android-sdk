package com.chartboost.sdk.privacy.usecase;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static com.chartboost.sdk.privacy.model.LGPD.LGPD_STANDARD;

import androidx.annotation.NonNull;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.privacy.PrivacyStandardRepository;
import com.chartboost.sdk.privacy.model.DataUseConsent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class GetWhitelistedPrivacyStandardsListUseCase {
    private final PrivacyStandardRepository mRepository;

    public GetWhitelistedPrivacyStandardsListUseCase(PrivacyStandardRepository repository) {
        mRepository = repository;
    }

    public List<DataUseConsent> execute(SdkConfiguration.PrivacyStandardsConfig config) {
        HashMap<String, DataUseConsent> data = mRepository.getMap();
        List<DataUseConsent> consents = filterGdpr(data);
        List<DataUseConsent> whitelisted = new ArrayList<>();
        HashSet<String> whitelist = getPrivacyStandardWhitelist(config);
        if (whitelist != null) {
            for (DataUseConsent consent : consents) {
                if (isConsentWhitelisted(whitelist, consent)) {
                    whitelisted.add(consent);
                }
            }
        } else {
            // in case there were configuration error CCPA should still be whitelisted
            if (data.containsKey(CCPA_STANDARD)) {
                whitelisted.add(data.get(CCPA_STANDARD));
            }

            // in case there were configuration error COPPA should still be whitelisted
            if (data.containsKey(COPPA_STANDARD)) {
                whitelisted.add(data.get(COPPA_STANDARD));
            }

            // in case there were configuration error LGPD should still be whitelisted
            if (data.containsKey(LGPD_STANDARD)) {
                whitelisted.add(data.get(LGPD_STANDARD));
            }
        }
        return whitelisted;
    }

    private HashSet<String> getPrivacyStandardWhitelist(SdkConfiguration.PrivacyStandardsConfig config) {
        if (config != null) {
            return config.getPrivacyStandardsWhitelist();
        }
        return null;
    }

    private boolean isConsentWhitelisted(@NonNull HashSet<String> whitelist, @NonNull DataUseConsent consent) {
        if (whitelist.contains(consent.getPrivacyStandard())) {
            return true;
        } else {
            Logger.e("DataUseConsent " + consent.getPrivacyStandard() + " is not whitelisted.", null);
            return false;
        }
    }

    private List<DataUseConsent> filterGdpr(HashMap<String, DataUseConsent> data) {
        HashMap<String, DataUseConsent> copy = new HashMap<>(data);
        copy.remove(GDPR_STANDARD);
        return new ArrayList<>(copy.values());
    }
}
