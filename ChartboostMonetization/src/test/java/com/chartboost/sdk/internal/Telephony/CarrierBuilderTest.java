package com.chartboost.sdk.internal.Telephony;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CarrierBuilderTest {

    private TelephonyManager telephonyManagerMock = mock(TelephonyManager.class);
    private Context contextMock = mock(Context.class);
    private CarrierBuilder carrierBuilder = new CarrierBuilder();

    private String simOperator = "123456";
    private String mnc = "456";
    private String mcc = "123";
    private String operatorName = "ORANGE NETWORK";
    private String countryISO = "ES";

    private int phoneType = TelephonyManager.PHONE_TYPE_CDMA;

    @Before
    public void setup() {
        when(contextMock.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManagerMock);
        when(telephonyManagerMock.getSimState()).thenReturn(TelephonyManager.SIM_STATE_READY);
        //5 or 6 decimal digits
        when(telephonyManagerMock.getSimOperator()).thenReturn(simOperator);
        when(telephonyManagerMock.getNetworkOperatorName()).thenReturn(operatorName);
        when(telephonyManagerMock.getNetworkCountryIso()).thenReturn(countryISO);
        when(telephonyManagerMock.getPhoneType()).thenReturn(phoneType);

        when(contextMock.checkPermission(anyString(), anyInt(), anyInt())).thenReturn(PERMISSION_GRANTED);
    }

    @Test
    public void buildCarrierPermissionsGrantedAndSimReadyTest() {
        Carrier carrier = carrierBuilder.build(contextMock);
        assertNotNull(carrier);
        assertEquals(simOperator, carrier.getSimOperator());
        assertEquals(mcc, carrier.getMccCode());
        assertEquals(mnc, carrier.getMncCode());
        assertEquals(operatorName, carrier.getNetworkOperatorName());
        assertEquals(phoneType, carrier.getPhoneType());

    }

    @Test
    public void buildCarrierPermissionsNotGrantedAndSimReadyTest() {
        when(contextMock.checkPermission(anyString(), anyInt(), anyInt())).thenReturn(PERMISSION_DENIED);
        Carrier carrier = carrierBuilder.build(contextMock);
        assertNull(carrier);
    }

    @Test
    public void buildCarrierPermissionsGrantedAndSimNotReadyTest() {
        when(telephonyManagerMock.getSimState()).thenReturn(TelephonyManager.SIM_STATE_NOT_READY);
        Carrier carrier = carrierBuilder.build(contextMock);
        assertNull(carrier);
    }
}
