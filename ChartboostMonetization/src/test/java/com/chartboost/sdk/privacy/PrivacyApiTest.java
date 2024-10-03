package com.chartboost.sdk.privacy;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
import com.chartboost.sdk.privacy.usecase.GetDataUseConsentUseCase;
import com.chartboost.sdk.privacy.usecase.GetPrivacyListAsJsonUseCase;
import com.chartboost.sdk.privacy.usecase.GetWhitelistedPrivacyStandardsListUseCase;
import com.chartboost.sdk.privacy.usecase.PutDataUseConsentUseCase;
import com.chartboost.sdk.privacy.usecase.RemoveDataUseConsentUseCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class PrivacyApiTest {

    private final PutDataUseConsentUseCase putDataUseConsentUseCase = mock(PutDataUseConsentUseCase.class);
    private final GetDataUseConsentUseCase getDataUseConsentUseCase = mock(GetDataUseConsentUseCase.class);
    private final RemoveDataUseConsentUseCase removeDataUseConsentUseCase = mock(RemoveDataUseConsentUseCase.class);
    private final GetPrivacyListAsJsonUseCase getPrivacyListAsJsonUseCase = mock(GetPrivacyListAsJsonUseCase.class);
    private final GetWhitelistedPrivacyStandardsListUseCase getWhitelistedPrivacyStandardsListUseCase = mock(GetWhitelistedPrivacyStandardsListUseCase.class);
    private final TCFv2 tcfv2 = mock(TCFv2.class);
    private final GPP gpp = mock(GPP.class);

    private PrivacyApi privacyApi;

    @Before
    public void setup() {
        privacyApi = new PrivacyApi(
                putDataUseConsentUseCase,
                getDataUseConsentUseCase,
                removeDataUseConsentUseCase,
                getPrivacyListAsJsonUseCase,
                getWhitelistedPrivacyStandardsListUseCase,
                tcfv2,
                gpp,
                ""
        );
    }

    @Test
    public void putPrivacyStandardTest() {
        DataUseConsent consent = mock(DataUseConsent.class);
        privacyApi.putPrivacyStandard(consent);
        verify(putDataUseConsentUseCase, times(1)).execute(consent);
    }

    @Test
    public void getPrivacyStandardTest() {
        String standard = "test";
        privacyApi.getPrivacyStandard(standard);
        verify(getDataUseConsentUseCase, times(1)).execute(standard);
    }

    @Test
    public void removePrivacyStandardTest() {
        String standard = "test";
        ArgumentCaptor<String> standardCapture = ArgumentCaptor.forClass(String.class);
        privacyApi.removePrivacyStandard(standard);
        verify(removeDataUseConsentUseCase, times(1)).execute(standardCapture.capture());
        String standardCaptured = standardCapture.getValue();
        assertEquals(standard, standardCaptured);
    }

    @Test
    public void getPrivacyListAsJsonTest() {
        ArgumentCaptor<List<DataUseConsent>> dataCaptor = ArgumentCaptor.forClass(List.class);
        SdkConfiguration.PrivacyStandardsConfig configMock = mock(SdkConfiguration.PrivacyStandardsConfig.class);
        privacyApi.setPrivacyConfig(configMock);
        DataUseConsent consent = new Custom("test", "test");
        List<DataUseConsent> data = new ArrayList<>();
        data.add(consent);

        when(getWhitelistedPrivacyStandardsListUseCase.execute(configMock)).thenReturn(data);

        privacyApi.getPrivacyListAsJson();
        verify(getPrivacyListAsJsonUseCase, times(1)).execute(dataCaptor.capture());
        List<DataUseConsent> listCaptured = dataCaptor.getValue();
        assertEquals(data.size(), listCaptured.size());
        DataUseConsent consentCaptured = listCaptured.get(0);
        assertEquals(consent, consentCaptured);
    }

    @Test
    public void getWhitelistedPrivacyStandardsListEmptyTest() {
        ArgumentCaptor<SdkConfiguration.PrivacyStandardsConfig> configCaptor = ArgumentCaptor.forClass(SdkConfiguration.PrivacyStandardsConfig.class);
        SdkConfiguration.PrivacyStandardsConfig configMock = mock(SdkConfiguration.PrivacyStandardsConfig.class);
        privacyApi.setPrivacyConfig(configMock);
        List<DataUseConsent> consents = privacyApi.getWhitelistedPrivacyStandardsList();

        assertNotNull(consents);
        assertEquals(0, consents.size());
        verify(getWhitelistedPrivacyStandardsListUseCase, times(1)).execute(configCaptor.capture());
        SdkConfiguration.PrivacyStandardsConfig configCaptured = configCaptor.getValue();
        assertEquals(configMock, configCaptured);
    }

    @Test
    public void getOpenRtbConsentForBehavioralGDPRConsent() {
        when(getDataUseConsentUseCase.execute(GDPR.GDPR_STANDARD)).thenReturn(new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL));
        int value = privacyApi.getOpenRtbConsent();
        assertEquals(1, value);
    }

    @Test
    public void getOpenRtbConsentTestForNonBehavioralGDPRConsent() {
        when(getDataUseConsentUseCase.execute(GDPR.GDPR_STANDARD)).thenReturn(new GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL));
        int value = privacyApi.getOpenRtbConsent();
        assertEquals(0, value);
    }

    @Test
    public void getOpenRtbConsentTestForUnknownConsent() {
        when(getDataUseConsentUseCase.execute(GDPR.GDPR_STANDARD)).thenReturn(null);
        int value = privacyApi.getOpenRtbConsent();
        assertEquals(0, value);
    }

    @Test
    public void getOpenRtbGdprTest() {
        int value = privacyApi.getOpenRtbGdpr();
        assertEquals(0, value);
        verify(getDataUseConsentUseCase, times(1)).execute(GDPR.GDPR_STANDARD);
    }
//
//
//    @Test
//    fun `add gdpr behavioral dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(GDPR_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("1", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add gdpr non behavioral dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(GDPR_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("0", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add ccpa opt in sale dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(CCPA_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("1YN-", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add ccpa opt out sale dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = CCPA(CCPA.CCPA_CONSENT.OPT_OUT_SALE)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(CCPA_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("1YY-", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add coppa true dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = COPPA(true)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(COPPA_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("true", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add coppa false dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = COPPA(false)
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals(COPPA_STANDARD, consentCaptured.privacyStandard)
//        assertEquals("false", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add custom valid test dataUseConsent`() {
//        val captor = ArgumentCaptor.forClass(DataUseConsent::class.java)
//        val saveConsent = Custom("test", "test")
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(1)).putPrivacyStandard(captor.capture())
//        val consentCaptured = captor.value
//        assertNotNull(consentCaptured)
//        assertEquals("test", consentCaptured.privacyStandard)
//        assertEquals("test", consentCaptured.consent.toString())
//    }
//
//    @Test
//    fun `add custom invalid dataUseConsent`() {
//        val saveConsent = Custom("test", "")
//        chartboostApi.addDataUseConsent(saveConsent)
//        verify(privacyApiMock, times(0)).putPrivacyStandard(any())
//    }
//
//    @Test
//    fun `get gdpr dataUseConsent`() {
//        val consent = GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL)
//        whenever(privacyApiMock.getPrivacyStandard(GDPR_STANDARD)).thenReturn(consent)
//        val result = chartboostApi.getDataUseConsent(GDPR_STANDARD)
//        assertEquals(consent, result)
//    }
//
//    @Test
//    fun `get ccpa dataUseConsent`() {
//        val consent = CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE)
//        whenever(privacyApiMock.getPrivacyStandard(CCPA_STANDARD)).thenReturn(consent)
//        val result = chartboostApi.getDataUseConsent(CCPA_STANDARD)
//        assertEquals(consent, result)
//    }
//
//    @Test
//    fun `get coppa dataUseConsent`() {
//        val consent = COPPA(true)
//        whenever(privacyApiMock.getPrivacyStandard(COPPA_STANDARD)).thenReturn(consent)
//        val result = chartboostApi.getDataUseConsent(COPPA_STANDARD)
//        assertEquals(consent, result)
//    }
//
//    @Test
//    fun `get custom dataUseConsent`() {
//        val consent = Custom("test", "test")
//        whenever(privacyApiMock.getPrivacyStandard("test")).thenReturn(consent)
//        val result = chartboostApi.getDataUseConsent("test")
//        assertEquals(consent, result)
//    }
//
//    @Test
//    fun `clear dataUseConsent by passing test value`() {
//        val captor = ArgumentCaptor.forClass(String::class.java)
//        chartboostApi.clearDataUseConsent("test")
//        verify(privacyApiMock, times(1)).removePrivacyStandard(captor.capture())
//        assertEquals("test", captor.value)
//    }
//
//    @Test
//    fun `clear dataUseConsent by passing empty string`() {
//        val captor = ArgumentCaptor.forClass(String::class.java)
//        chartboostApi.clearDataUseConsent("")
//        verify(privacyApiMock, times(1)).removePrivacyStandard(captor.capture())
//        assertEquals("", captor.value)
//    }
}
