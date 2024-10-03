package com.chartboost.sdk.privacy;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class PrivacyStandardRepositoryTest {

    private static final String PRIVACY_STANDARD_KEY = "privacyStandard";
    private static final String CONSENT_KEY = "consent";
    private SharedPreferences prefs = mock(SharedPreferences.class);
    private SharedPreferences.Editor edit = mock(SharedPreferences.Editor.class);
    private EventTracker eventTrackerMock = mock(EventTracker.class);
    private PrivacyStandardRepository repository;

    @Before
    public void setup() {
        when(prefs.edit()).thenReturn(edit);
        when(prefs.getString(anyString(), anyString())).thenReturn("{}");
        when(edit.putString(anyString(), anyString())).thenReturn(edit);

        repository = new PrivacyStandardRepository(prefs, eventTrackerMock);
    }

    @Test
    public void getPrivacyStandardGDPRTest() {
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        repository.put(gdpr);
        DataUseConsent getGdpr = repository.getGDPR();
        assertEquals(gdpr, getGdpr);
    }

    @Test
    public void getPrivacyStandardsMapTest() {
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        DataUseConsent ccpa = new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE);
        DataUseConsent coppa = new COPPA(true);

        DataUseConsent custom = new Custom("test", "test");

        repository.put(gdpr);
        repository.put(ccpa);
        repository.put(coppa);
        repository.put(custom);

        HashMap<String, DataUseConsent> consents = repository.getMap();
        DataUseConsent loadedGdpr = consents.get(GDPR_STANDARD);
        DataUseConsent loadedCCPA = consents.get(CCPA_STANDARD);
        DataUseConsent loadedCOPPA = consents.get(COPPA_STANDARD);

        DataUseConsent loadedCustom = consents.get("test");

        assertEquals(gdpr, loadedGdpr);
        assertEquals(ccpa, loadedCCPA);
        assertEquals(coppa, loadedCOPPA);
        assertEquals(custom, loadedCustom);
    }

    @Test
    public void removePrivacyStandardTest() {
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        repository.put(gdpr);
        repository.remove(GDPR_STANDARD);
        DataUseConsent removeGdpr = repository.getMap().get(GDPR_STANDARD);
        assertNull(removeGdpr);
    }

    @Test
    public void savePrivacyStandardsTest() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        String privacyStandard = "test";
        String consent = "test";
        SharedPreferences.Editor editorMock = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editorMock);
        when(editorMock.putString(anyString(), anyString())).thenReturn(editorMock);

        JSONArray jsonarray = new JSONArray();
        DataUseConsent customDataUseConsent = new Custom(privacyStandard, consent);
        jsonarray.put(toJson(customDataUseConsent.getPrivacyStandard(), customDataUseConsent.getConsent()));
        String jsonExpected = jsonarray.toString();
        repository.put(customDataUseConsent);
        verify(prefs, times(1)).edit();
        verify(editorMock, times(1)).putString(keyCaptor.capture(), valueCaptor.capture());
        verify(editorMock, times(1)).apply();

        String key = keyCaptor.getValue();
        String value = valueCaptor.getValue();

        assertEquals("privacy_standards", key);
        assertEquals(jsonExpected, value);
    }

    @Test
    public void loadPrivacyStandardsTest() {
        String privacyStandard = "test";
        String consent = "test";
        JSONArray jsonarray = new JSONArray();
        DataUseConsent customDataUseConsent = new Custom(privacyStandard, consent);
        jsonarray.put(customDataUseConsent);

        repository.put(customDataUseConsent);
        HashMap<String, DataUseConsent> consents = repository.getMap();
        DataUseConsent loadedConsent = consents.get(privacyStandard);
        assertEquals(1, consents.size());
        assertNotNull(loadedConsent);
        assertEquals(customDataUseConsent.getPrivacyStandard(), loadedConsent.getPrivacyStandard());
        assertEquals(customDataUseConsent.getConsent(), loadedConsent.getConsent());
    }

    @Test
    public void loadPrivacyStandardsGDPRTest() {
        JSONArray jsonarray = new JSONArray();
        DataUseConsent gdpr = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        jsonarray.put(gdpr);

        repository.put(gdpr);
        HashMap<String, DataUseConsent> consents = repository.getMap();
        DataUseConsent loadedConsent = consents.get(GDPR_STANDARD);
        assertEquals(1, consents.size());
        assertNotNull(loadedConsent);
        assertEquals(gdpr.getPrivacyStandard(), loadedConsent.getPrivacyStandard());
        assertEquals(gdpr.getConsent(), loadedConsent.getConsent());
    }

    @Test
    public void loadPrivacyStandardsMultipleTest() {
        JSONArray jsonarray = new JSONArray();
        DataUseConsent gdprConsent = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
        jsonarray.put(gdprConsent);

        String privacyStandard = "test";
        String consent = "test";
        DataUseConsent customDataUseConsent = new Custom(privacyStandard, consent);
        jsonarray.put(customDataUseConsent);

        repository.put(gdprConsent);
        repository.put(customDataUseConsent);
        HashMap<String, DataUseConsent> consents = repository.getMap();
        DataUseConsent loadedConsentGDPR = consents.get(GDPR_STANDARD);
        DataUseConsent loadedConsentCustom = consents.get(privacyStandard);

        assertEquals(2, consents.size());
        assertNotNull(loadedConsentGDPR);
        assertEquals(gdprConsent.getPrivacyStandard(), loadedConsentGDPR.getPrivacyStandard());
        assertEquals(gdprConsent.getConsent(), loadedConsentGDPR.getConsent());

        assertNotNull(loadedConsentCustom);
        assertEquals(customDataUseConsent.getPrivacyStandard(), loadedConsentCustom.getPrivacyStandard());
        assertEquals(customDataUseConsent.getConsent(), loadedConsentCustom.getConsent());
    }

    private JSONObject toJson(String privacyStandardName, Object consentValue) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PRIVACY_STANDARD_KEY, privacyStandardName);
            jsonObject.put(CONSENT_KEY, consentValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
