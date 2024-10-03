package com.chartboost.sdk.internal.Networking;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.internal.ChartboostDSPHelper;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBCrypto;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.ConfigurationBodyFields;
import com.chartboost.sdk.internal.Model.DeviceBodyFields;
import com.chartboost.sdk.internal.Model.IdentityBodyFields;
import com.chartboost.sdk.internal.Model.MediationBodyFields;
import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.ReachabilityBodyFields;
import com.chartboost.sdk.internal.Model.RequestBodyFields;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.internal.Model.SessionBodyFields;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Networking.requests.NetworkType;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer;
import com.chartboost.sdk.internal.identity.TrackingState;
import com.chartboost.sdk.test.ChartboostDSPMockerKt;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.test.TestContainerControl;
import com.chartboost.sdk.test.TestDisplayMetrics;
import com.chartboost.sdk.test.TestUtils;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

@RunWith(MockitoJUnitRunner.class)
public class CBRequestTest {

    private final EventTracker eventTrackerMock = mock(EventTracker.class);

    private TestContainer webViewTestContainer() {
        return TestContainer.defaultWebView();
    }

    private TestContainer webViewTestContainer(TestContainerControl control) {
        return new TestContainerBuilder(control).build();
    }

    @Before
    public void setup() {
        ChartboostDependencyContainer.INSTANCE.start("appid", "signature");
    }

    @Test
    public void testNoConfigVariantByDefault() {
        try (TestContainer tc = webViewTestContainer()) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();

            JSONObject body = req.body;
            assertFalse(body.has("config_variant"));
        }
    }

    @Test
    public void testNoConfigVariantIfEmptyString() {
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant("");
        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();

            JSONObject body = req.body;
            assertFalse(body.has("config_variant"));
        }
    }

    @Test
    public void testConfigVariantIfSet() {
        final String configVariant = "some string value";
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant(configVariant);

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();

            assertThat(req.body.optString("config_variant"), is(equalTo(configVariant)));
        }
    }

    /*
        The config_variant field should not be present in the submitted request body
        if it's not set.
     */
    @Test
    public void testConfigVariantNotPresentInSubmittedRequestIfNotSet() {
        //JSONObject body = getBodyForRequestWithConfigVariant(null);
        //assertFalse(body.has("config_variant"));
    }

    /*
        The config_variant field should not be present in the submitted request body
        if it's empty.
     */
    @Test
    public void testConfigVariantNotPresentInSubmittedRequestIfEmpty() {
        //JSONObject body = getBodyForRequestWithConfigVariant("");
        //assertFalse(body.has("config_variant"));
    }

    /*
        The config_variant field should be present in the submitted request body
        if it's present in the configuration.
     */
    @Test
    public void testConfigVariantPresentInSubmittedRequest() {
        String configVariant = "some string value";
        JSONObject body = getBodyForRequestWithConfigVariant(configVariant);
        assertThat(body.optString("config_variant"), is(equalTo(configVariant)));
    }

    private JSONObject getBodyForRequestWithConfigVariant(String configVariant) {
        TestContainerControl control = new TestContainerControl();
        if (configVariant != null)
            control.configure().withConfigVariant(configVariant);

        try (TestContainer tc = new TestContainerBuilder(control)
                .build()) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            String body = new String(Objects.requireNonNull(req.buildRequestInfo()).body);
            return new JSONObject(body);
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    @Test
    public void signature() throws JSONException {
        final ResponseDescriptor response = ReferenceResponse.interstitialShowImpressionRecorded;

        try (TestContainer tc = TestContainerBuilder.defaultWebView()
                .withResponse(response)
                .build()) {

            final String uri = response.endpoint.uri;
            RequestBodyFields requestBodyFields = tc.requestBodyBuilder.build();
            CBRequest request = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "/interstitial/show",
                    requestBodyFields,
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );

            tc.networkService.submit(request);
            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable(); // the /interstitial/show request

            final String s = request.body.toString();
            JSONObject body = new JSONObject(s);
            assertThat(body.getString("identity"), is(requestBodyFields.getIdentityBodyFields().getIdentifiers()));

            String appSignature = tc.getAppSignature();
            String description = String.format(Locale.US, "%s %s\n%s\n%s",
                    "POST", "/interstitial/show", appSignature, request.body.toString());
            String signature = CBCrypto.getSha1Hex(description);

            HttpsURLConnection prefetchConn = tc.mockNetworkFactory.getMockConnectionReturnedForRequest(
                    CBNetworkRequest.Method.POST, uri);

            verify(prefetchConn).addRequestProperty("X-Chartboost-Signature", signature);
        }
    }


    @Test
    public void bodyParameters() throws JSONException {
        int[] seeds = {1, 4352, 56747, 24124, 34523, 5685756, 4565, 2354235, 45674657, 2342};
        for (int seed : seeds) {
            Random r = new Random(seed);
            boolean portrait = r.nextBoolean();

            float density = r.nextFloat();
            TestDisplayMetrics displayMetrics =
                    (portrait ? TestDisplayMetrics.portrait() : TestDisplayMetrics.landscape())
                            .withDensity(density)
                            .withDensityDpi(r.nextInt(1000) + 1);
            TestContainerControl control = TestContainerControl.defaultNative();
            control.setDisplayMetrics(displayMetrics);

            String mediation = r.nextBoolean()
                    ? (r.nextBoolean() ? "mediation1" : "mediation2")
                    : null;
            String mediationVersion = mediation != null
                    ? (r.nextBoolean() ? "1.2.7" : "8.2.3")
                    : null;

            int sessionCount = 23;
            try (TestContainer tc = webViewTestContainer(control)) {
                JSONObject privacyObject = new JSONObject();
                privacyObject.put("us_privacy", "1YY-");
                privacyObject.put("test_privacy", "test_consent");

                SdkConfiguration.PrivacyStandardsConfig configMock = mock(SdkConfiguration.PrivacyStandardsConfig.class);
                tc.privacyApi.setPrivacyConfig(configMock);
                when(tc.privacyApi.toPrivacyBodyFields()).thenReturn(new PrivacyBodyFields(0, null, 0,0,privacyObject, null, "", "", ""));

                SessionBodyFields sessionBodyFields = new SessionBodyFields(
                        "1",0,sessionCount,0,0,0
                );
                when(tc.session.toSessionBodyFields()).thenReturn(sessionBodyFields);

                MediationBodyFields mediationBodyFields = new Mediation(mediation, mediationVersion, mediationVersion).toMediationBodyFields();
                when(tc.mediation.toMediationBodyFields()).thenReturn(mediationBodyFields);

                RequestBodyFields requestBodyFields = tc.requestBodyBuilder.build();
                CBRequest request = new CBRequest(
                        CBConstants.API_ENDPOINT,
                        "a/b/c",
                        requestBodyFields,
                        Priority.NORMAL,
                        null,
                        eventTrackerMock
                );
                request.appendRequestBodyInfoParams();

                JSONObject body = request.body;
                assertThat((String) body.get("app"), is(tc.appId));
                assertThat((String) body.get("bundle"), is(tc.control.constants.packageVersionName));
                assertThat((String) body.get("bundle_id"), is(tc.control.constants.packageName));
                assertTrue(body.has("carrier"));

                JSONObject carrier = body.getJSONObject("carrier");
                assertNotNull(carrier);
                assertThat((String) body.get("country"), is(Locale.getDefault().getCountry()));
                assertThat((String) body.get("device_type"), is("null null"));
                assertThat((String) body.get("make"), is("unknown"));
                assertThat((String) body.get(CBConstants.REQUEST_PARAM_ACTUAL_DEVICE_TYPE), is("phone"));
                assertThat((Integer) body.get("dh"), is(1024));
                assertThat((String) body.get("dpi"), is(Integer.toString(848)));
                assertThat((Integer) body.get("dw"), is(768));
                assertThat((Integer) body.get("h"), is(1024));
                assertTrue(body.has("identity"));
                assertThat((Boolean) body.get("is_portrait"), is(true));
                assertThat((String) body.get("language"), is(Locale.getDefault().getLanguage()));
                // oh boy, this doesn't change after first read (baked into RequestBody)
                assertThat((Integer) body.get("connectiontype"), is(NetworkType.CELLULAR_UNKNOWN.getValue()));

                if (mediation == null) {
                    assertFalse(body.has("mediation"));
                    assertNull(body.opt("mediation"));
                } else {
                    assertTrue(body.has("mediation"));
                    assertThat((String) body.get("mediation"), is(mediation + " " + mediationVersion));
                }

                assertFalse(body.has("model")); // only because it's not set in local unit tests

                // Build.VERSION.RELEASE is null in unit tests
                assertThat((String) body.get("os"), is("Android null"));
                assertThat((Integer) body.get("reachability"), is(requestBodyFields.getReachabilityBodyFields().getConnectionTypeFromActiveNetwork()));
                assertThat((Float) body.get("scale"), is(1f));
                assertThat((String) body.get("sdk"), is(BuildConfig.SDK_VERSION));
                assertThat((Integer) body.get("session"), is(sessionCount));

                int timestamp = Integer.parseInt((String) body.get("timestamp"));
                int now = (int) TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
                int slop = (int) TimeUnit.MINUTES.toSeconds(10);
                assertThat(timestamp, is(both(greaterThan(now - slop)).and(lessThan(now + slop))));
                assertTrue(body.has("timezone"));
                assertThat((Integer) body.get("w"), is(768));
                assertEquals(body.get("privacy"), privacyObject);
            }
        }
    }

    @Test
    public void limitAdTracking() throws JSONException {
        for (Boolean limited : Arrays.asList(null, Boolean.TRUE, Boolean.FALSE)) {
            try (TestContainer tc = TestContainer.defaultWebView()) {
                TrackingState trackingState = TrackingState.TRACKING_UNKNOWN;
                if (limited != null) trackingState = limited ?  TrackingState.TRACKING_LIMITED : TrackingState.TRACKING_ENABLED;

                IdentityBodyFields identityBodyFields = new IdentityBodyFields(trackingState, "the hex identifiers", "a uuid", "a gaid", "a setId", 1);
                SessionBodyFields sessionBodyFields = new SessionBodyFields("",0,0,0,0,0);
                ReachabilityBodyFields reachability = new ReachabilityBodyFields(4, 0, "0", NetworkType.CELLULAR_2G);
                ConfigurationBodyFields configurationBodyFields = new ConfigurationBodyFields("variant", true, "");
                DeviceBodyFields deviceBodyFields = new DeviceBodyFields();
                MediationBodyFields mediationBodyFields = new Mediation("mediation", "lVersion","aVersion").toMediationBodyFields();
                RequestBodyFields requestBodyFields = new RequestBodyFields(tc.appId, tc.getAppSignature(), identityBodyFields, reachability, tc.carrierBuilder.build(tc.applicationContext), sessionBodyFields, tc.testTimeSource.toBodyFields(), tc.privacyApi.toPrivacyBodyFields(), configurationBodyFields, deviceBodyFields, mediationBodyFields);

                CBRequest request = new CBRequest(
                        CBConstants.API_ENDPOINT,
                        "a/b/c",
                        requestBodyFields,
                        Priority.NORMAL,
                        null,
                        eventTrackerMock
                );
                request.appendRequestBodyInfoParams();

                JSONObject body = request.body;

                assertFalse(body.has("tracking"));

                if (limited == null) {
                    assertFalse(body.has("limit_ad_tracking"));
                } else {
                    assertThat((Boolean) body.get("limit_ad_tracking"), is(limited));
                }
            }
        }
    }

    @Test
    public void timestampChanges() throws InterruptedException {
        try (TestContainer tc = TestContainer.defaultWebView()) {
            int timestamp1 = getNewRequestTimestamp(tc);

            int epoch1 = tc.testTimeSource.epochTime();
            tc.testTimeSource.advanceUptime(1100, TimeUnit.MILLISECONDS);
            int epoch2 = tc.testTimeSource.epochTime();

            int timestamp2 = getNewRequestTimestamp(tc);

            assertThat(timestamp1, is(epoch1));
            assertThat(timestamp2, is(epoch2));
            assertThat(timestamp2, is(greaterThan(timestamp1)));
        }
    }

    int getNewRequestTimestamp(TestContainer tc) {
        CBRequest request = new CBRequest(
                CBConstants.API_ENDPOINT,
                "a/b/c",
                tc.requestBodyBuilder.build(),
                Priority.NORMAL,
                null,
                eventTrackerMock
        );
        request.appendRequestBodyInfoParams();
        JSONObject body = request.body;
        return Integer.parseInt(body.optString("timestamp"));
    }

    @Test
    public void sendsToSessionLogsIffTrackPresent() {
        for (boolean success : TestUtils.eitherBoolean) {
            // TODO expectTracked never used?
            for (boolean expectTracked : TestUtils.eitherBoolean) {
                try (TestContainer tc = webViewTestContainer()) {
                    CBRequest req = new CBRequest(
                            CBConstants.API_ENDPOINT,
                            "a/b/c",
                            tc.requestBodyBuilder.build(),
                            Priority.NORMAL,
                            null,
                            eventTrackerMock
                    );

                    if (success)
                        req.deliverResponse(mock(JSONObject.class), mock(CBNetworkServerResponse.class));
                    else
                        req.deliverError(
                                new CBError(
                                        CBError.Internal.MISCELLANEOUS,
                                        "error message"
                                ),
                                mock(CBNetworkServerResponse.class)
                        );
                }
            }
        }
    }

    @Test
    public void dspEnabledValidTest() {
        final String configVariant = "some string value";
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant(configVariant);

        ChartboostDSPMockerKt.mockChartboostDSP(true);
        ChartboostDSPHelper.INSTANCE.setDspCode("R2D2");
        ChartboostDSPHelper.INSTANCE.setDspCreatives(new int[]{1});

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();
            CBNetworkRequestInfo info = req.buildRequestInfo();
            assertNotNull(info.headers);
            String dspHeader = info.headers.get(CBConstants.REQUEST_PARAM_HEADER_DSP_KEY);
            assertNotNull(dspHeader);

            int exchangeMode = 0;
            double bidFloor = 0;
            String dspCode = "";
            JSONArray forceCreativeTypes = new JSONArray();
            int creativeValue = 0;
            try {
                JSONObject jsonHeader = new JSONObject(dspHeader);
                exchangeMode = jsonHeader.getInt("exchangeMode");
                bidFloor = jsonHeader.getDouble("bidFloor");
                dspCode = jsonHeader.getString("code");
                forceCreativeTypes = jsonHeader.getJSONArray("forceCreativeTypes");
                creativeValue = forceCreativeTypes.getInt(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            assertEquals(2, exchangeMode);
            assertEquals(0.01, bidFloor, 3);
            assertEquals(ChartboostDSPHelper.INSTANCE.getDspCode(), dspCode);
            assertEquals(ChartboostDSPHelper.INSTANCE.getDspCreatives()[0], creativeValue);
        }
    }

    @Test
    public void dspEnabledInvalidCodeTest() {
        final String configVariant = "some string value";
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant(configVariant);

        ChartboostDSPMockerKt.mockChartboostDSP(true);
        ChartboostDSPHelper.INSTANCE.setDspCode("");
        ChartboostDSPHelper.INSTANCE.setDspCreatives(new int[]{1});

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();
            CBNetworkRequestInfo info = req.buildRequestInfo();
            assertNotNull(info.headers);
            String dspHeader = info.headers.get(CBConstants.REQUEST_PARAM_HEADER_DSP_KEY);
            assertNotNull(dspHeader);
            assertEquals("{}", dspHeader);
        }
    }

    @Test
    public void dspEnabledInvalidCreativeTest() {
        final String configVariant = "some string value";
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant(configVariant);

        ChartboostDSPMockerKt.mockChartboostDSP(true);
        ChartboostDSPHelper.INSTANCE.setDspCode("R2D2");
        ChartboostDSPHelper.INSTANCE.setDspCreatives(new int[]{});

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();
            CBNetworkRequestInfo info = req.buildRequestInfo();
            assertNotNull(info.headers);
            String dspHeader = info.headers.get(CBConstants.REQUEST_PARAM_HEADER_DSP_KEY);
            assertNotNull(dspHeader);
            assertEquals("{}", dspHeader);
        }
    }

    @Test
    public void dspDisabledTest() {
        final String configVariant = "some string value";
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant(configVariant);

        ChartboostDSPMockerKt.mockChartboostDSP(false);
        ChartboostDSPHelper.INSTANCE.setDspCode("R2D2");
        ChartboostDSPHelper.INSTANCE.setDspCreatives(new int[]{1});

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            req.appendRequestBodyInfoParams();
            CBNetworkRequestInfo info = req.buildRequestInfo();
            assertNotNull(info.headers);
            String dspHeader = info.headers.get(CBConstants.REQUEST_PARAM_HEADER_DSP_KEY);
            assertNull(dspHeader);
        }
    }

    @Test
    public void appendBodyArgumentIdentityTest() {
        TestContainerControl control = new TestContainerControl();
        control.configure().withConfigVariant("some string value");

        try (TestContainer tc = webViewTestContainer(control)) {
            CBRequest request = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "a/b/c",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );
            request.appendRequestBodyInfoParams();
            String identity = request.body.optString("identity");
            //expects value without next line param at the end of the value
            assertEquals("eyJnYWlkIjoiODI4YWFmZTgtYjU2NS00ODFjLThhMGEtMTBjYTFhZDFhYmUxIn0=", identity);
        }
    }

}
