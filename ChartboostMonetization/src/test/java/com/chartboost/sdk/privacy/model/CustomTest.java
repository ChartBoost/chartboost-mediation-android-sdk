package com.chartboost.sdk.privacy.model;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import junit.framework.TestCase;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CustomTest {

    @Test(expected = NullPointerException.class)
    public void createCustomNull() {
        Custom custom = new Custom(null, null);
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test(expected = NullPointerException.class)
    public void createCustomStandardNull() {
        Custom custom = new Custom(null, "test");
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test(expected = NullPointerException.class)
    public void createCustomConsentNull() throws Exception {
        Custom custom = new Custom("test", null);
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test
    public void createCustomEmpty() {
        Custom custom = new Custom("", "");
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test
    public void customTest215Chars() {
        String privacyStandard = "privacyStanddnewjkdnewkjcnewjkarddkmeklwdmeklwmdeklwmdklewmfeklwmcfeklwmceklwcmeklwcmewklcmewklcmweklcwemclkewmcvlkewmcvklewmcvklewmvlkewmveklwmvewklvmewklmewklmfceklwmcveklwmvwkvemwklvrmevlkermvlkermveklrvmerlkvmer";
        String consent = "test";
        int valueSize = privacyStandard.length();
        TestCase.assertEquals(215, valueSize);
        Custom custom = new Custom(privacyStandard, consent);
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test
    public void customTest100Chars() {
        String privacyStandard = "privacyStanddnewjkdnewkjcnewjkarddkmeklwdmeklwmdeklwmdklewmfeklwmcfeklwmceklwcmeklwcmewklcmewklcmwek";
        String consent = "test";
        int valueSize = privacyStandard.length();
        TestCase.assertEquals(100, valueSize);
        Custom custom = new Custom(privacyStandard, consent);
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test
    public void customTest99Chars() throws JSONException {
        String privacyStandard = "privacyStanddnewjkdnewkjcnewjkarddkmeklwdmeklwmdeklwmdklewmfeklwmcfeklwmceklwcmeklwcmewklcmewklcmwe";
        String consent = "test";
        int valueSize = privacyStandard.length();
        TestCase.assertEquals(99, valueSize);

        DataUseConsent dataUseConsent = new Custom(privacyStandard, consent);
        String jsonStandard = dataUseConsent.getPrivacyStandard();
        String jsonConsent = (String) dataUseConsent.getConsent();
        int valueSizeAfterTrim = jsonStandard.length();
        TestCase.assertEquals(jsonStandard, privacyStandard);
        TestCase.assertEquals(jsonConsent, consent);
        TestCase.assertEquals(99, valueSizeAfterTrim);
    }

    @Test
    public void createCustomGDPR() {
        Custom custom = new Custom(GDPR_STANDARD, "test");
        assertNotNull(custom);
        assertEquals(custom.getPrivacyStandard(), "");
        assertEquals(custom.getConsent(), "");
    }

    @Test
    public void createCustomCCPA() {
        Custom custom = new Custom(CCPA_STANDARD, "test");
        assertNotNull(custom);
        assertEquals(CCPA_STANDARD, custom.getPrivacyStandard());
        assertEquals("test", custom.getConsent());
    }

    @Test
    public void createCustomValid() {
        Custom custom = new Custom("test", "test");
        assertNotNull(custom);
        assertEquals("test", custom.getPrivacyStandard());
        assertEquals("test", custom.getConsent());
    }
}
