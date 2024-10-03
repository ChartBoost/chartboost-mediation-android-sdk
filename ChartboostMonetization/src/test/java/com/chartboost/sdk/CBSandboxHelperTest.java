package com.chartboost.sdk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.legacy.Factory;
import com.chartboost.sdk.test.SdkConfigurationBuilder;
import com.chartboost.sdk.test.TestContainer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Iterator;

public class CBSandboxHelperTest extends BaseTest {
    @Test
    public void overrideConfig_BasicOverride() {
        try (TestContainer tc = new TestContainer()) {
            final String baseConfigVariant = "base variant";
            final int baseWebViewCacheMaxBytes = 772;
            final int overrideWebViewCacheMaxUnits = 42;

            JSONObject base = new SdkConfigurationBuilder()
                    .withWebViewEnabled(false)
                    .withConfigVariant(baseConfigVariant)
                    .withWebViewCacheMaxBytes(baseWebViewCacheMaxBytes)
                    .options();

            JSONObject overrides = new SdkConfigurationBuilder()
                    .withWebViewEnabled(true)
                    .withWebViewCacheMaxUnits(overrideWebViewCacheMaxUnits)
                    .options();

            // config overrides live here:
            tc.control.sharedPreferenceValues.put("configOverrides", overrides.toString());

            JSONObject combined = overrideConfig(base, tc.sharedPreferences);
            SdkConfiguration sdkConfig = new SdkConfiguration(combined);

            // the values we are specifically overriding should be overridden...
            assertTrue(sdkConfig.webviewEnabled);
            assertThat(sdkConfig.webviewCacheMaxUnits, is(overrideWebViewCacheMaxUnits));

            // top-level values not overridden should come from the base wrapper
            assertThat(sdkConfig.configVariant, is(baseConfigVariant));

            // sub-object values not overridden should come from the base wrapper
            assertThat(sdkConfig.webviewCacheMaxBytes, is(baseWebViewCacheMaxBytes));

            // the base JSONWrapper should not be modified
            assertFalse(new SdkConfiguration(base).webviewEnabled);

            // the overrides JSONWrapper should not be modified
            assertThat(new SdkConfiguration(overrides).configVariant, is(""));
        }
    }

    public static JSONObject overrideConfig(JSONObject configFromServer, SharedPreferences sharedPrefs) {
        try {
            String configOverridesJson = sharedPrefs.getString("configOverrides", "{}");

            JSONObject configOverrides = new JSONObject(configOverridesJson);

            JSONObject configWithOverrides = new JSONObject();
            overwriteFields(configFromServer, configWithOverrides);
            overwriteFields(configOverrides, configWithOverrides);

            return configWithOverrides;
        } catch (JSONException ignored) {
            return configFromServer;
        }
    }

    private static void overwriteFields(JSONObject src, JSONObject dst) throws JSONException {
        Factory factory = Factory.instance();
        Iterator<String> keys = src.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object srcField = src.get(key);
            if (srcField instanceof JSONObject) {
                JSONObject dstField = dst.optJSONObject(key);
                if (dstField == null) {
                    dstField = new JSONObject();
                    dst.put(key, dstField);
                }
                overwriteFields((JSONObject) srcField, dstField);
            } else {
                dst.put(key, srcField);
            }
        }
    }

    /*
        overrideConfig should bring over values from the base config
        if there are no overrides.

        Also, JSONObject.keys() returns null for an empty JSONObject,
        and this test would fail if that null pointer check were missing.
     */
    @Test
    public void overrideConfig_NoOverrides() {
        try (TestContainer tc = new TestContainer()) {
            final String baseConfigVariant = "base variant";

            JSONObject base = new SdkConfigurationBuilder()
                    .withWebViewEnabled(true)
                    .withConfigVariant(baseConfigVariant)
                    .options();

            SdkConfiguration sdkConfig = new SdkConfiguration(base);

            // bring in values from the base json wrapper
            assertTrue(sdkConfig.webviewEnabled);
            assertThat(sdkConfig.configVariant, is(baseConfigVariant));

            // and bring in defaults
            assertThat(sdkConfig.webviewCacheMaxBytes, is(CBConstants.DEFAULT_WEBVIEW_CACHE_MAX_BYTES));
        }
    }
}
