package com.chartboost.sdk.internal.Networking;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.annotation.NonNull;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBCrypto;
import com.chartboost.sdk.internal.identity.TrackingState;
import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.SessionBodyFields;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest;
import com.chartboost.sdk.internal.Networking.requests.NetworkType;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.test.TestContainerControl;
import com.chartboost.sdk.test.TestDisplayMetrics;
import com.chartboost.sdk.test.TestUtils;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class CBWebViewRequestTest extends BaseTest {

    private final EventTracker eventTrackerMock = mock(EventTracker.class);

    @Before
    public void setup() {
        ChartboostDependencyContainer.INSTANCE.start("appid", "signature");
    }

    private TestContainer webViewTestContainer() {
        return TestContainer.defaultWebView();
    }

    private TestContainer webViewTestContainer(TestContainerControl control) {
        return new TestContainerBuilder(control).build();
    }

    /*
        The config_variant field should not be present in the submitted request body
        if it's not set.
     */
    @Test
    public void testConfigVariantNotPresentInSubmittedRequestIfNotSet() throws JSONException {
        JSONObject body = getBodyForRequestWithConfigVariant(null);
        assertFalse(body.getJSONObject("sdk").has("config_variant"));
    }

    /*
        The config_variant field should not be present in the submitted request body
        if it's empty.
     */
    @Test
    public void testConfigVariantNotPresentInSubmittedRequestIfEmpty() throws JSONException {
        JSONObject body = getBodyForRequestWithConfigVariant("");
        assertFalse(body.getJSONObject("sdk").has("config_variant"));
    }

    /*
        The config_variant field should be present in the submitted request body
        if it's present in the configuration.
     */
    @Test
    public void testConfigVariantPresentInSubmittedRequest() throws JSONException {
        String configVariant = "some string value";
        JSONObject body = getBodyForRequestWithConfigVariant(configVariant);
        assertThat(body.getJSONObject("sdk").optString("config_variant"), is(equalTo(configVariant)));
    }

    private JSONObject getBodyForRequestWithConfigVariant(String configVariant) {
        TestContainerControl containerControl = new TestContainerControl();
        if (configVariant != null)
            containerControl.configure()
                    .withConfigVariant(configVariant);

        try (TestContainer tc = webViewTestContainer(containerControl)) {
            CBWebViewRequest req = createTestCBWebViewRequest(tc);

            return new JSONObject(new String(req.buildRequestInfo().body));
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    @NonNull
    private CBWebViewRequest createTestCBWebViewRequest(TestContainer tc) {
        CBWebViewRequest req = new CBWebViewRequest(
                "a/b/c",
                tc.requestBodyBuilder.build(),
                Priority.NORMAL,
                null,
                eventTrackerMock
        );

        // appendRequestBodyInfoParams blows up if no body arguments have been added yet
        req.appendBodyArgument(CBConstants.REQUEST_PARAM_ASSET_LIST, new JSONObject());

        return req;
    }

    @Test
    public void bodyParameters() {
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

            int sessionCount = 23;

            try (TestContainer tc = webViewTestContainer(control)) {
                when(tc.session.toSessionBodyFields()).thenReturn(
                        new SessionBodyFields(
                            null,
                                0,
                                sessionCount,
                                0,
                                0,
                                0
                        )
                );
                JSONObject privacyObject = new JSONObject();
                privacyObject.put("us_privacy", "1YY-");
                privacyObject.put("test_privacy", "test_consent");

                when(tc.privacyApi.toPrivacyBodyFields()).thenReturn(new PrivacyBodyFields(
                        0, null, 0, 0, privacyObject, "-1)", "", "", "")
                );

                CBWebViewRequest request = new CBWebViewRequest(
                        "a/b/c",
                        tc.requestBodyBuilder.build(),
                        Priority.NORMAL,
                        null,
                        eventTrackerMock
                );

                // webview appendRequestBodyInfoParams blows up if the body doesn't already exist
                request.appendBodyArgument("dummy_key", "dummy_value");
                request.appendRequestBodyInfoParams();

                JSONObject body = request.body;
                assertThat(TestUtils.toStringList(body.names()), containsInAnyOrder(
                        "ad", "app", "bidrequest", "device", "sdk", "dummy_key"));

                JSONObject ad = body.optJSONObject("ad");
                JSONObject app = body.optJSONObject("app");
                JSONObject device = body.optJSONObject("device");
                JSONObject sdk = body.optJSONObject("sdk");

                // "ad" sub-object
                assertThat(TestUtils.toStringList(ad.names()),
                        containsInAnyOrder("amount", "cache", "location", "retry_count", "session"));
                assertThat((Integer) ad.get("amount"), is(0));
                assertFalse((Boolean) ad.get("cache"));
                assertThat((String) ad.get("location"), is(""));
                assertThat((Integer) ad.get("retry_count"), is(0));
                assertThat((Integer) ad.get("session"), is(sessionCount));

                // "app" sub-object
                List<String> expectedAppFields = new ArrayList<>(Arrays.asList("app", "bundle", "bundle_id", "session_id", "test_mode", "ui"));
                assertThat(TestUtils.toStringList(app.names()),
                        containsInAnyOrder(expectedAppFields.toArray(new String[0])));
                assertThat((String) app.get("app"), is(tc.appId));
                assertThat((String) app.get("bundle"), is(tc.control.constants.packageVersionName));
                assertThat((String) app.get("bundle_id"), is(tc.control.constants.packageName));
                assertThat((String) app.get("session_id"), is(""));
                assertFalse((Boolean) app.get("test_mode"));
                assertThat((Integer) app.get("ui"), is(-1));

                // "device" sub-object
                List<String> expectedDeviceFields = Arrays.asList(
                        "carrier", "country", "device_family", "make", "device_type", "model",
                        CBConstants.REQUEST_PARAM_ACTUAL_DEVICE_TYPE, "dh", "dpi", "dw", "h",
                        "identity", "is_portrait", "language", "limit_ad_tracking", "pidatauseconsent",
                        "connectiontype", "os", "reachability", "retina", "scale", "timestamp",
                        "timezone", "w", "user_agent", "consent", "privacy", "appsetidscope"
                );

                assertThat(
                        TestUtils.toStringList(device.names()),
                        containsInAnyOrder(expectedDeviceFields.toArray(new String[0]))
                );

                assertThat((String) device.get("country"), is(Locale.getDefault().getCountry()));
                assertThat((String) device.get("make"), is("unknown"));
                assertThat((String) device.get("device_type"), is("unknown robolectric"));
                assertThat((String) device.get("actual_device_type"), is("phone"));
                assertThat((String) device.get(CBConstants.REQUEST_PARAM_ACTUAL_DEVICE_TYPE), is("phone"));
                assertThat((Integer) device.get("dh"), is(1024));
                assertThat((String) device.get("dpi"), is(Integer.toString(848)));
                assertThat((Integer) device.get("dw"), is(768));
                assertThat((Integer) device.get("h"), is(1024));
                assertThat((Boolean) device.get("is_portrait"), is(true));
                assertThat((String) device.get("language"), is(Locale.getDefault().getLanguage()));
                // oh boy, this doesn't change after first read (baked into RequestBody)
                assertThat((Integer) device.get("connectiontype"), is(NetworkType.CELLULAR_UNKNOWN.getValue()));
                assertThat((String) device.get("os"), is("Android " + Build.VERSION.RELEASE));
                assertThat((Integer) device.get("reachability"), is(tc.requestBodyBuilder.build().getReachabilityBodyFields().getConnectionTypeFromActiveNetwork()));
                assertThat((Float) device.get("scale"), is(1f));
                assertThat((String) device.get("model"), is("robolectric"));
                Integer aScope = 1;
                assertThat((Integer) device.get("appsetidscope"), is(aScope));

                int timestamp = Integer.parseInt((String) device.get("timestamp"));
                int now = (int) TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
                int slop = (int) TimeUnit.MINUTES.toSeconds(10);
                assertThat(timestamp, is(both(greaterThan(now - slop)).and(lessThan(now + slop))));

                assertThat((Integer) device.get("w"), is(768));

                // "sdk" sub-object:
                List<String> expectedSdkFields = new ArrayList<>(Arrays.asList("commit_hash", "sdk"));

                assertThat(TestUtils.toStringList(sdk.names()),
                        containsInAnyOrder(expectedSdkFields.toArray(new String[0])));

                assertThat((String) sdk.get("sdk"), is(BuildConfig.SDK_VERSION));

                // {"carrier_name":"Verizon Wireless","mobile_country_code":"311","mobile_network_code":"480","iso_country_code":"us","phone_type":2}
                JSONObject carrier = device.getJSONObject("carrier");
                assertThat(TestUtils.toStringList(carrier.names()), containsInAnyOrder(
                        "carrier_name", "mobile_country_code", "mobile_network_code",
                        "iso_country_code", "phone_type"));
                assertThat((String) carrier.get("carrier_name"), is(tc.control.constants.carrierName));
                assertThat((String) carrier.get("mobile_country_code"), is(tc.control.constants.carrierMobileCountryCode));
                assertThat((String) carrier.get("mobile_network_code"), is(tc.control.constants.carrierMobileNetworkCode));
                assertThat((String) carrier.get("iso_country_code"), is(tc.control.constants.carrierIsoCountryCode));
                assertThat((Integer) carrier.get("phone_type"), is(tc.control.constants.carrierPhoneType));
                JSONObject privacy = device.getJSONObject("privacy");
                assertEquals(device.getString("consent"), "");
                assertThat(TestUtils.toStringList(privacy.names()), containsInAnyOrder(
                        "us_privacy", "test_privacy", "gpp", "gpp_sid"));
                assertEquals(privacy.getString("us_privacy"), "1YY-");
                assertEquals(privacy.getString("test_privacy"), "test_consent");

            } catch (JSONException ex) {
                throw new Error(ex);
            }
        }
    }

    @Test
    public void limitAdTracking() throws JSONException {
        for (Boolean limited : Arrays.asList(null, Boolean.TRUE, Boolean.FALSE)) {
            try (TestContainer tc = webViewTestContainer()) {
                TrackingState trackingState = TrackingState.TRACKING_UNKNOWN;
                if (limited != null)
                    trackingState = limited ? TrackingState.TRACKING_LIMITED : TrackingState.TRACKING_ENABLED;

                tc.setIdentityTracking(trackingState);

                CBRequest request = new CBWebViewRequest(
                        "a/b/c",
                        tc.requestBodyBuilder.build(),
                        Priority.NORMAL,
                        null,
                        eventTrackerMock
                );
                // webview appendRequestBodyInfoParams blows up if you body doesn't already exist
                request.appendBodyArgument("dummy_key", "dummy_value");
                request.appendRequestBodyInfoParams();

                JSONObject body = request.body;

                JSONObject device = body.optJSONObject("device");

                if (limited == null) {
                    assertFalse(device.has("limit_ad_tracking"));
                } else {
                    assertThat((Boolean) device.get("limit_ad_tracking"), is(limited));
                }
            }
        }
    }

    @Test
    public void timestampChanges() throws InterruptedException {
        try (TestContainer tc = webViewTestContainer()) {
            int timestamp1 = getNewRequestTimestamp(tc);

            tc.testTimeSource.advanceUptime(1100, TimeUnit.MILLISECONDS);

            int timestamp2 = getNewRequestTimestamp(tc);

            assertThat(timestamp2, is(greaterThan(timestamp1)));
        }
    }

    int getNewRequestTimestamp(TestContainer tc) {
        CBRequest request1 = new CBWebViewRequest(
                "a/b/c",
                tc.requestBodyBuilder.build(),
                Priority.NORMAL,
                null,
                eventTrackerMock
        );
        // webview appendRequestBodyInfoParams blows up if you body doesn't already exist
        request1.appendBodyArgument("dummy_key", "dummy_value");
        request1.appendRequestBodyInfoParams();

        JSONObject body1 = request1.body;
        return Integer.parseInt(body1.optJSONObject("device").optString("timestamp"));
    }

    @Test
    public void signature() throws JSONException {
        final ResponseDescriptor response = ReferenceResponse.webviewV2PrefetchWithResults;

        try (TestContainer tc = TestContainerBuilder.defaultWebView()
                .withResponse(response)
                .build()) {

            final String uri = response.endpoint.uri;
            final String identifiers = "eyJnYWlkIjoiODI4YWFmZTgtYjU2NS00ODFjLThhMGEtMTBjYTFhZDFhYmUxIn0=";
            CBWebViewRequest request = new CBWebViewRequest(
                    "/webview/v2/prefetch",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    null,
                    eventTrackerMock
            );


            tc.networkService.submit(request);

            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable(); // the /webview/v2/prefetch request

            HttpsURLConnection prefetchConn = tc.mockNetworkFactory.getMockConnectionReturnedForRequest(
                    CBNetworkRequest.Method.POST, uri);

            // CBRequestManager has already calculated the signature.  This is about to change.
            final String s = request.body.toString();
            JSONObject body = new JSONObject(s);
            assertThat(body.getJSONObject("device").getString("identity"), is(identifiers));
            String description = String.format(Locale.US, "%s %s\n%s\n%s",
                    "POST", "/webview/v2/prefetch", tc.getAppSignature(), s);
            String signature = CBCrypto.getSha1Hex(description);


            verify(prefetchConn).addRequestProperty("X-Chartboost-Signature", signature);
        }
    }
}
