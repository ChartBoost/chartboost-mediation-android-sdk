package com.chartboost.sdk.internal.Model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Libraries.CBJSON;
import com.chartboost.sdk.legacy.CBConfig;
import com.chartboost.sdk.test.AndroidTestContainer;
import com.chartboost.sdk.test.AndroidTestContainerBuilder;
import com.chartboost.sdk.test.SdkConfigurationBuilder;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SdkConfigurationTest extends BaseTest {
    @Test
    public void testDefaults() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {

            SdkConfiguration defaultConfig = new SdkConfiguration(new JSONObject());

            assertIsDefaultConfiguration(defaultConfig);
        }
    }

    public static void assertIsDefaultConfiguration(SdkConfiguration defaultConfig) {
        assertThat(defaultConfig.configVariant, is(""));
        assertFalse(defaultConfig.publisherDisable);

        assertTrue(defaultConfig.trackCritical);
        assertFalse(defaultConfig.trackDebug);
        assertFalse(defaultConfig.trackSession);
        assertFalse(defaultConfig.trackSystem);
        assertFalse(defaultConfig.trackUser);

        assertThat(defaultConfig.webviewCacheMaxBytes, is(100 * 1024 * 1024));
        assertTrue(defaultConfig.webviewEnabled);
        assertTrue(defaultConfig.webviewInPlayEnabled);
        assertThat(defaultConfig.webviewInvalidatePendingImpressionTTLMinutes, is(3));
        assertTrue(defaultConfig.webviewLockOrientation);
        assertThat(defaultConfig.webviewPrefetchSession, is(3));
        assertTrue(defaultConfig.webviewRewardVideoEnabled);
        assertThat(defaultConfig.webviewVersion, is("v2"));
    }

    @Test
    public void configurePublisherDisabled() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPublisherDisable(v);
                assertThat(builder.build().publisherDisable, is(v));
            }
        }
    }

    @Test
    public void configureConfigVariant_asThroughApiConfig() {
        List<String> values = Arrays.asList("a", "b", "", "z", "a", "");
        try (TestContainer tc = TestContainer.emptyConfig()) {
            for (String v : values) {
                JSONObject configWrapper = new JSONObject();
                CBJSON.put(configWrapper, "configVariant", v);

                assertTrue(CBConfig.updateConfig(tc.sdkConfig, configWrapper));

                assertThat(tc.sdkConfig.get().configVariant, is(equalTo(v)));
            }
        }
    }

    @Test
    public void configureWebviewCacheMaxBytes() {
        List<Integer> expectedValues = Arrays.asList(0, 650, 1800000000);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewCacheMaxBytes(v);
                assertThat(builder.build().webviewCacheMaxBytes, is(v));
            }
        }
    }

    @Test
    public void configureWebviewCacheMaxUnits() {
        List<Integer> expectedValues = Arrays.asList(0, 1, 5, 18, 21);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewCacheMaxUnits(v);

                int expectedValue = (v > 0) ? v : 10;
                assertThat(builder.build().webviewCacheMaxUnits, is(expectedValue));
            }
        }
    }

    @Test
    public void configureWebviewInvalidatePendingImpression() {
        List<Integer> expectedValues = Arrays.asList(0, 1, 5, 18, 21);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewInvalidatePendingImpressionTTLMinutes(v);

                int expectedValue = (v > 0) ? v : 3;
                assertThat(builder.build().webviewInvalidatePendingImpressionTTLMinutes, is(expectedValue));
            }
        }
    }

    @Test
    public void configureWebviewCacheTTL() {
        List<Integer> expectedValues = Arrays.asList(1, 5, 6);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewCacheTTLDays(v);
                assertThat(builder.build().webviewCacheTTLDays, is(v));
            }
        }
    }

    @Test
    public void configureWebviewDirectories() throws JSONException {
        List<List<String>> expectedValues =
                Arrays.asList(
                        Arrays.asList("videos", "templates"),
                        Arrays.asList("abc", "def", "ghi", "videos")
                );

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (List<String> v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewDirectories(v);
                assertThat(builder.build().webviewDirectories, is(v));
            }
        }
    }

    @Test
    public void configureInvalidateFolderList() throws JSONException {
        List<List<String>> expectedValues =
                Arrays.asList(
                        Arrays.asList("videos", "templates"),
                        Arrays.asList("abc", "def", "ghi", "videos")
                );

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (List<String> v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withInvalidateFolderList(v);
                assertThat(builder.build().invalidateFolderList, is(v));
            }
        }
    }

    @Test
    public void configureWebviewEnabled() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewEnabled(v);
                assertThat(builder.build().webviewEnabled, is(v));
            }
        }
    }

    @Test
    public void configureWebViewInPlayEnabled() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewInPlayEnabled(v);
                assertThat(builder.build().webviewInPlayEnabled, is(v));
            }
        }
    }

    @Test
    public void configureWebViewInterstitialEnabled() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewInterstitialEnabled(v);
                assertThat(builder.build().webviewInterstitialEnabled, is(v));
            }
        }
    }

    @Test
    public void configureWebViewLockOrientation() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewLockOrientation(v);
                assertThat(builder.build().webviewLockOrientation, is(v));
            }
        }
    }

    @Test
    public void configureWebviewPrefetchSession() {
        List<Integer> expectedValues = Arrays.asList(1, 5, 6);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewPrefetchSession(v);
                assertThat(builder.build().webviewPrefetchSession, is(v));
            }
        }
    }

    @Test
    public void configureWebviewRewardVideoEnabled() {
        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Boolean v : TestUtils.anyBooleanTransition) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewRewardVideoEnabled(v);
                assertThat(builder.build().webviewRewardVideoEnabled, is(v));
            }
        }
    }

    @Test
    public void configureWebviewVersion() {
        List<String> expectedValues = Arrays.asList("v2", "v3", "vN");

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (String v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewVersion(v);
                assertThat(builder.build().webviewVersion, is(v));
            }
        }
    }

    @Test
    public void configureWebviewEndpoints() {
        List<String> expectedValues = Arrays.asList("v2", "v3", "vN");

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (String version : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withWebViewVersion(version);
                String expectedPrefetchEndpoint = String.format("webview/%s/prefetch", version);
                assertThat(builder.build().webviewPrefetchEndpoint, is(expectedPrefetchEndpoint));
            }
        }
    }


    @Test
    public void verifyWebViewEnableByDefault() {
        HashMap<String, Boolean> osTestMatrix = new HashMap<>();
        osTestMatrix.put("", false);
        osTestMatrix.put("invalid.release.number", false);
        osTestMatrix.put("invalid.release.number.RC10", false);
        osTestMatrix.put("2.4.3", false);
        osTestMatrix.put("3.4", false);
        osTestMatrix.put("4.4.1", false);
        osTestMatrix.put("4.4.2", false);
        osTestMatrix.put("4.10.2", true);
        osTestMatrix.put("5", true);
        osTestMatrix.put("6.0.1", true);
        osTestMatrix.put("7.1", true);
        osTestMatrix.put("10.0.1", true);


        for (Map.Entry<String, Boolean> osVersionEntry : osTestMatrix.entrySet()) {
            try (AndroidTestContainer tc = new AndroidTestContainerBuilder()
                    .withOsReleaseVersion(osVersionEntry.getKey())
                    .build()) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder();

                final boolean actual = builder.build().webviewEnabled;
                assertThat(osVersionEntry.getKey(), actual, is(osVersionEntry.getValue()));
            }
        }
    }

    @Test
    public void configurePrecacheBufferSize() {
        List<Integer> expectedValues = Arrays.asList(0, 100, 3, -1);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheBufferSize(v);
                assertThat(builder.build().getPrecacheConfig().getBufferSize(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheMaxBytes() {
        List<Long> expectedValues = Arrays.asList(0L, 650L, 1800000000L);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Long v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheMaxBytes(v);
                assertThat(builder.build().getPrecacheConfig().getMaxBytes(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheMaxUnitsPerTimeWindow() {
        List<Integer> expectedValues = Arrays.asList(0, 650, 1800000000);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheMaxUnitsPerTimeWindow(v);
                assertThat(builder.build().getPrecacheConfig().getMaxUnitsPerTimeWindow(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheMaxUnitsPerTimeWindowCellular() {
        List<Integer> expectedValues = Arrays.asList(0, 650, 1800000000);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Integer v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheMaxUnitsPerTimeWindowCellular(v);
                assertThat(builder.build().getPrecacheConfig().getMaxUnitsPerTimeWindowCellular(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheTimeWindow() {
        List<Long> expectedValues = Arrays.asList(0L, 650L, 1800000000L);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Long v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheTimeWindow(v);
                assertThat(builder.build().getPrecacheConfig().getTimeWindow(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheTimeWindowCellular() {
        List<Long> expectedValues = Arrays.asList(0L, 650L, 1800000000L);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Long v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheTimeWindowCellular(v);
                assertThat(builder.build().getPrecacheConfig().getTimeWindowCellular(), is(v));
            }
        }
    }

    @Test
    public void configurePrecacheTTL() {
        List<Long> expectedValues = Arrays.asList(0L, 650L, 1800000000L);

        try (AndroidTestContainer tc = new AndroidTestContainer()) {
            for (Long v : expectedValues) {
                SdkConfigurationBuilder builder = new SdkConfigurationBuilder().withPreCacheTTL(v);
                assertThat(builder.build().getPrecacheConfig().getTtl(), is(v));
            }
        }
    }
}
