package com.chartboost.sdk.internal.Networking;


import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.os.Build.VERSION_CODES.P;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.chartboost.sdk.test.AndroidTestContainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {P})
public class CBReachabilityTest {

    @Test
    public void testConnectionSuccessWithCapabilities() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);
            when(tc.activeNetworkCapability.hasCapability(anyInt())).thenReturn(true);
            assertTrue(reachability.isNetworkAvailable());
        }
    }

    @Test
    public void testSetConnectionTypeWifi() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);

            when(tc.activeNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

            reachability.connectionTypeFromActiveNetwork();
            assertTrue(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_WIFI));
        }
    }

    @Test
    public void testSetConnectionTypeMobile() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);

            when(tc.activeNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);

            reachability.connectionTypeFromActiveNetwork();
            assertTrue(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_MOBILE));
        }
    }

    @Test
    public void testSetConnectionTypeDisconnected() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);

            when(tc.activeNetworkInfo.isConnected()).thenReturn(false);
            when(tc.activeNetworkCapability.hasCapability(anyInt())).thenReturn(false);

            reachability.connectionTypeFromActiveNetwork();
            assertFalse(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_ERROR));
        }
    }

    @Test
    public void testSetConnectionTypeNoDefaultNetwork() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {

            CBReachability reachability = new CBReachability(tc.applicationContext);
            assertTrue(reachability.isNetworkAvailable());
            when(tc.activeNetworkCapability.hasCapability(anyInt())).thenReturn(false);
            when(tc.connectivityManager.getActiveNetworkInfo()).thenReturn(null);
            reachability.connectionTypeFromActiveNetwork();
            assertFalse(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_ERROR));
        }
    }

    /*
        getActiveNetworkInfo can throw a SecurityException if android.permission.ACCESS_NETWORK_STATE is not set
     */
    @Test
    public void testSetConnectionTypeNoAccess() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);

            assertTrue(reachability.isNetworkAvailable());

            // connectivity manager cannot be accessed
            when(tc.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);

            reachability.connectionTypeFromActiveNetwork();

            assertFalse(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_ERROR));
        }
    }

    /*
        getActiveNetworkInfo can throw a SecurityException if android.permission.ACCESS_NETWORK_STATE is not set
     */
    @Test
    public void testSetConnectionTypeNoAccessPreviouslyNetworkUnavailable() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            tc.grantPermission(ACCESS_NETWORK_STATE);
            CBReachability reachability = new CBReachability(tc.applicationContext);

            when(tc.activeNetworkInfo.isConnected()).thenReturn(false);
            when(tc.activeNetworkCapability.hasCapability(anyInt())).thenReturn(false);

            reachability.connectionTypeFromActiveNetwork();
            assertFalse(reachability.isNetworkAvailable());

            // connectivity manager cannot be accessed
            when(tc.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);
            reachability.connectionTypeFromActiveNetwork();

            // note that this does not change isNetworkAvailable!
            assertFalse(reachability.isNetworkAvailable());
            assertThat(
                    reachability.connectionTypeFromActiveNetwork(),
                    is(ConnectionType.CONNECTION_ERROR)
            );
        }
    }

    @Test
    public void testNetworkType() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            NetworkInfo infoMock = mock(NetworkInfo.class);
            CBReachability reachability = new CBReachability(tc.applicationContext);

            assertEquals(reachability.cellularConnectionType(), TelephonyManager.NETWORK_TYPE_UNKNOWN);

            when(tc.connectivityManager.getActiveNetworkInfo()).thenReturn(infoMock);
            when(infoMock.isConnected()).thenReturn(true);
            when(infoMock.getSubtype()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_A);

            assertEquals(reachability.cellularConnectionType(), TelephonyManager.NETWORK_TYPE_EVDO_A);

        }
    }

    @Test
    public void testNetworkTypeNoTelephonyManager() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            CBReachability reachability = new CBReachability(tc.applicationContext);
            when(tc.applicationContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null);
            assertThat(reachability.cellularConnectionType(), is(0));
        }
    }
}
