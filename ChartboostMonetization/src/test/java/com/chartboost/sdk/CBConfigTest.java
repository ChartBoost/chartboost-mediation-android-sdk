package com.chartboost.sdk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.legacy.CBConfig;
import com.chartboost.sdk.test.SdkConfigurationBuilder;
import com.chartboost.sdk.test.SimulatedException;
import com.chartboost.sdk.test.TestContainer;

import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class CBConfigTest extends BaseTest {

    @Test
    public void updateConfig() {
        try (TestContainer tc = new TestContainer()) {
            SdkConfigurationBuilder builder = new SdkConfigurationBuilder();
            AtomicReference<SdkConfiguration> sdkConfig = new AtomicReference<>();
            assertTrue(CBConfig.updateConfig(sdkConfig, builder.options()));
            assertTrue(sdkConfig.get().webviewEnabled);

            builder.withWebViewEnabled(false);
            assertTrue(CBConfig.updateConfig(sdkConfig, builder.options()));
            assertFalse(sdkConfig.get().webviewEnabled);
        }
    }

    /*
        updateConfig should change nothing if the SdkConfiguration constructor throws.
     */
    @Test
    public void processConfigFallbackToDefault() {
        try (TestContainer tc = new TestContainer()) {
            tc.control.configure().withConfigVariant("a config variant");
            tc.installConfig();
            assertThat(tc.sdkConfig.get().configVariant, is(equalTo("a config variant")));

            JSONObject brokenConfig = mock(JSONObject.class, RETURNS_DEEP_STUBS);
            when(brokenConfig.optString(anyString())).thenThrow(new SimulatedException());
            when(brokenConfig.keys()).thenThrow(new SimulatedException());
            assertFalse(CBConfig.updateConfig(tc.sdkConfig, brokenConfig));
            assertThat(tc.sdkConfig.get().configVariant, is(equalTo("a config variant")));
        }

    }

}
