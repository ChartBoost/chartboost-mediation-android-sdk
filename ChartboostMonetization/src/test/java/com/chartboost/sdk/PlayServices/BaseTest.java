package com.chartboost.sdk.PlayServices;

import android.os.Build;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.gms.Shadows;
import org.robolectric.shadows.gms.common.ShadowGoogleApiAvailability;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows = {ShadowGoogleApiAvailability.class}, sdk = {Build.VERSION_CODES.P})
public abstract class BaseTest {
    @Before
    public void setUp() {
        final ShadowGoogleApiAvailability shadowGoogleApiAvailability = Shadows.shadowOf(GoogleApiAvailability.getInstance());
        final int expectedCode = ConnectionResult.SUCCESS;
        shadowGoogleApiAvailability.setIsGooglePlayServicesAvailable(expectedCode);
    }
}