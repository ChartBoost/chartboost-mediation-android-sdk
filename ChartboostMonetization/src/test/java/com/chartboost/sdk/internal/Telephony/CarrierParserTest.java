package com.chartboost.sdk.internal.Telephony;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;

import com.chartboost.sdk.internal.Libraries.CBConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CarrierParserTest {

    private String simOperator = "123456";
    private String mnc = "456";
    private String mcc = "123";
    private String operatorName = "ORANGE NETWORK";
    private String countryISO = "ES";
    private int phoneType = TelephonyManager.PHONE_TYPE_CDMA;

    private Carrier carrier = mock(Carrier.class);
    private CarrierParser parser = new CarrierParser();

    @Before
    public void setup() {
        when(carrier.getMccCode()).thenReturn(mcc);
        when(carrier.getMncCode()).thenReturn(mnc);
        when(carrier.getNetworkCountryIso()).thenReturn(countryISO);
        when(carrier.getNetworkOperatorName()).thenReturn(operatorName);
        when(carrier.getPhoneType()).thenReturn(phoneType);
    }

    @Test
    public void parseValid() throws JSONException {
        JSONObject json = parser.parseCarrierToJsonObject(carrier);
        String actualName = json.getString(CBConstants.REQUEST_PARAM_CARRIER_NAME);
        String actualMcc = json.getString(CBConstants.REQUEST_PARAM_MCC);
        String actualMnc = json.getString(CBConstants.REQUEST_PARAM_MNC);
        String actualIso = json.getString(CBConstants.REQUEST_PARAM_ISO);
        int actualPhoneType = json.getInt(CBConstants.REQUEST_PARAM_PHONE_TYPE);

        assertEquals(operatorName, actualName);
        assertEquals(mcc, actualMcc);
        assertEquals(mnc, actualMnc);
        assertEquals(countryISO, actualIso);
        assertEquals(phoneType, actualPhoneType);
    }

    @Test
    public void parseNull() {
        JSONObject json = parser.parseCarrierToJsonObject(null);
        assertNotNull(json);
        assertEquals(0, json.length());
    }
}
