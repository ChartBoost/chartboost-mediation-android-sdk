package com.chartboost.sdk.privacy.usecase;

import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.privacy.PrivacyStandardRepository;
import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class GetDataUseConsentUseCaseTest {

    private PrivacyStandardRepository repository = mock(PrivacyStandardRepository.class);
    private GetDataUseConsentUseCase usecase = new GetDataUseConsentUseCase(repository);

    @Test
    public void getDataUseConsentUseCaseTest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        String privacyStandard = "test";
        DataUseConsent custom = new Custom(privacyStandard, "test");
        consents.put(privacyStandard, custom);

        when(repository.getMap()).thenReturn(consents);
        DataUseConsent consentFromRepo = usecase.execute(privacyStandard);
        verify(repository, times(1)).getMap();
        assertEquals(custom, consentFromRepo);
    }

    @Test
    public void getDataUseConsentUseCaseGDPRTest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        consents.put(GDPR_STANDARD, gdpr);

        when(repository.getMap()).thenReturn(consents);
        DataUseConsent consentFromRepo = usecase.execute(GDPR_STANDARD);
        verify(repository, times(1)).getMap();
        assertEquals(gdpr, consentFromRepo);
    }


    @Test
    public void getDataUseConsentUseCaseMultipleEntryTest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        consents.put(GDPR_STANDARD, gdpr);

        DataUseConsent custom = new Custom("test", "test");
        consents.put(GDPR_STANDARD, gdpr);
        consents.put("test", custom);

        when(repository.getMap()).thenReturn(consents);
        DataUseConsent consentGDPRFromRepo = usecase.execute(GDPR_STANDARD);
        DataUseConsent consentCustomFromRepo = usecase.execute("test");

        verify(repository, times(2)).getMap();
        assertEquals(gdpr, consentGDPRFromRepo);
        assertEquals(custom, consentCustomFromRepo);

    }

    @Test
    public void getDataUseConsentUseCaseEmptyTest() {
        HashMap<String, DataUseConsent> consents = new HashMap<>();
        when(repository.getMap()).thenReturn(consents);
        DataUseConsent consentFromRepo = usecase.execute(GDPR_STANDARD);
        verify(repository, times(1)).getMap();
        assertNull(consentFromRepo);
    }
}
