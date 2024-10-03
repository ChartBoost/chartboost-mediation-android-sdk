package com.chartboost.sdk.privacy.model;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DataUseConsentImplTest {

    private static final String PRIVACY_STANDARD_KEY = "privacyStandard";
    private static final String CONSENT_KEY = "consent";

    @Test
    public void getPrivacyStandardTest() {
        String privacyStandard = "test";
        String consent = "test";
        DataUseConsent dataUseConsent = new Custom(privacyStandard, consent);
        String getPrivacyStandard = dataUseConsent.getPrivacyStandard();
        assertEquals(getPrivacyStandard, privacyStandard);
    }

    @Test
    public void getConsentTest() {
        String privacyStandard = "test";
        String consent = "test";
        DataUseConsent dataUseConsent = new Custom(privacyStandard, consent);
        String getConsent = (String) dataUseConsent.getConsent();
        assertEquals(getConsent, consent);
    }

    @Test
    public void toJsonTest() throws JSONException {
        String privacyStandard = "test";
        String consent = "test";
        DataUseConsent dataUseConsent = new Custom(privacyStandard, consent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        String jsonConsent = json.getString("consent");
        assertEquals(jsonStandard, privacyStandard);
        assertEquals(jsonConsent, consent);
    }

    @Test
    public void gdprValidValueBehavioralTest() throws JSONException {
        String consent = GDPR.GDPR_CONSENT.BEHAVIORAL.getValue();
        DataUseConsent dataUseConsent = new GDPR(GDPR.GDPR_CONSENT.fromValue(consent));
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        String jsonConsent = json.getString("consent");
        assertEquals(jsonStandard, GDPR_STANDARD);
        assertEquals(jsonConsent, consent);
    }

    @Test
    public void gdprValidValueNonBehavioralTest() throws JSONException {
        String consent = GDPR.GDPR_CONSENT.NON_BEHAVIORAL.getValue();
        DataUseConsent dataUseConsent = new GDPR(GDPR.GDPR_CONSENT.fromValue(consent));
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        String jsonConsent = json.getString("consent");
        assertEquals(jsonStandard, GDPR_STANDARD);
        assertEquals(jsonConsent, consent);
    }

    @Test(expected = NullPointerException.class)
    public void gdprInvalidValueTest() {
        new GDPR(GDPR.GDPR_CONSENT.fromValue("invalid value"));
    }

    @Test
    public void ccpaValidValueOptInTest() throws JSONException {
        String consent = CCPA.CCPA_CONSENT.OPT_IN_SALE.getValue();
        DataUseConsent dataUseConsent = new CCPA(CCPA.CCPA_CONSENT.fromValue(consent));
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        String jsonConsent = json.getString("consent");
        assertEquals(jsonStandard, CCPA_STANDARD);
        assertEquals(jsonConsent, consent);
    }

    @Test
    public void ccpaValidValueOptOutTest() throws JSONException {
        String consent = CCPA.CCPA_CONSENT.OPT_OUT_SALE.getValue();
        DataUseConsent dataUseConsent = new CCPA(CCPA.CCPA_CONSENT.fromValue(consent));
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        String jsonConsent = json.getString("consent");
        assertEquals(jsonStandard, CCPA_STANDARD);
        assertEquals(jsonConsent, consent);
    }

    @Test(expected = NullPointerException.class)
    public void ccpaInvalidValueTest() {
        new CCPA(CCPA.CCPA_CONSENT.fromValue("invalid value"));
    }

    @Test
    public void coppaValidValueOptInTest() throws JSONException {
        boolean isChildDirected = true;
        DataUseConsent dataUseConsent = new COPPA(isChildDirected);
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        boolean jsonConsent = json.getBoolean("consent");
        assertEquals(jsonStandard, COPPA_STANDARD);
        assertTrue(jsonConsent);
    }

    @Test
    public void coppaValidValueOptOutTest() throws JSONException {
        boolean isChildDirected = false;
        DataUseConsent dataUseConsent = new COPPA(isChildDirected);
        assertNotNull(dataUseConsent);
        JSONObject json = toJson(dataUseConsent);
        String jsonStandard = json.getString("privacyStandard");
        boolean jsonConsent = json.getBoolean("consent");
        assertEquals(jsonStandard, COPPA_STANDARD);
        assertFalse(jsonConsent);
    }

    private JSONObject toJson(DataUseConsent dataUseConsent) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PRIVACY_STANDARD_KEY, dataUseConsent.getPrivacyStandard());
            jsonObject.put(CONSENT_KEY, dataUseConsent.getConsent());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
