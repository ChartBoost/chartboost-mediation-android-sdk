package com.chartboost.sdk.privacy.usecase;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.privacy.PrivacyStandardRepository;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GetWhitelistedPrivacyStandardsTest {

    private PrivacyStandardRepository repository = mock(PrivacyStandardRepository.class);
    private GetWhitelistedPrivacyStandardsListUseCase usecase = new GetWhitelistedPrivacyStandardsListUseCase(repository);

    @Test
    public void getWhitelistedPrivacyStandardsListWhitelistIsNullTest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        String customPrivacyStandard = "test";
        String customConsent = "test_consent";

        GDPR gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        CCPA ccpa = new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE);
        Custom custom = new Custom(customPrivacyStandard, customConsent);
        consents.put(GDPR_STANDARD, gdpr);
        consents.put(CCPA_STANDARD, ccpa);
        consents.put(customPrivacyStandard, custom);
        when(repository.getMap()).thenReturn(consents);

        List<DataUseConsent> whitelistedData = usecase.execute(null);
        assertNotNull(whitelistedData);
        assertEquals(1, whitelistedData.size());
        DataUseConsent whitelistedCCPA = whitelistedData.get(0);
        assertEquals(ccpa, whitelistedCCPA);
    }

    @Test
    public void getWhitelistedPrivacyStandardsListWhitelistIsNullNoCCPATest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        String customPrivacyStandard = "test";
        String customConsent = "test_consent";

        GDPR gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        Custom custom = new Custom(customPrivacyStandard, customConsent);
        consents.put(GDPR_STANDARD, gdpr);
        consents.put(customPrivacyStandard, custom);
        when(repository.getMap()).thenReturn(consents);

        List<DataUseConsent> whitelistedData = usecase.execute(null);
        assertNotNull(whitelistedData);
        assertEquals(0, whitelistedData.size());
    }

    @Test
    public void getWhitelistedPrivacyStandardsListWhitelistAllTest() {
        SdkConfiguration.PrivacyStandardsConfig config = mock(SdkConfiguration.PrivacyStandardsConfig.class);
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        String customPrivacyStandard = "test";
        String customConsent = "test_consent";

        GDPR gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        CCPA ccpa = new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE);
        Custom custom = new Custom(customPrivacyStandard, customConsent);
        consents.put(GDPR_STANDARD, gdpr);
        consents.put(CCPA_STANDARD, ccpa);
        consents.put(customPrivacyStandard, custom);

        HashSet<String> whitelist = new HashSet<>();
        whitelist.add(GDPR_STANDARD);
        whitelist.add(CCPA_STANDARD);
        whitelist.add(customPrivacyStandard);

        when(repository.getMap()).thenReturn(consents);
        when(config.getPrivacyStandardsWhitelist()).thenReturn(whitelist);

        List<DataUseConsent> whitelistedData = usecase.execute(config);
        assertNotNull(whitelistedData);
        assertEquals(2, whitelistedData.size());

        DataUseConsent whitelistedCustom = whitelistedData.get(1);
        assertEquals(custom, whitelistedCustom);

        DataUseConsent whitelistedCCPA = whitelistedData.get(0);
        assertEquals(ccpa, whitelistedCCPA);
    }

    @Test
    public void getWhitelistedPrivacyStandardsListWhitelistEmptyTest() {
        SdkConfiguration.PrivacyStandardsConfig config = mock(SdkConfiguration.PrivacyStandardsConfig.class);
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        String customPrivacyStandard = "test";
        String customConsent = "test_consent";

        GDPR gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        CCPA ccpa = new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE);
        Custom custom = new Custom(customPrivacyStandard, customConsent);
        consents.put(GDPR_STANDARD, gdpr);
        consents.put(CCPA_STANDARD, ccpa);
        consents.put(customPrivacyStandard, custom);

        HashSet<String> whitelist = new HashSet<>();

        when(repository.getMap()).thenReturn(consents);
        when(config.getPrivacyStandardsWhitelist()).thenReturn(whitelist);

        List<DataUseConsent> whitelistedData = usecase.execute(config);
        assertNotNull(whitelistedData);
        assertEquals(0, whitelistedData.size());
    }
}
