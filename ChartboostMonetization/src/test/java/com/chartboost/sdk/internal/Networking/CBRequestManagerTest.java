package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.test.CustomMatchers.isCBInternalError;
import static com.chartboost.sdk.test.JSONObjectMatcher.equalsJSONObject;
import static com.chartboost.sdk.test.ReferenceResponse.badJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import androidx.annotation.NonNull;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.CBError.Internal;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Networking.requests.CBRequest.CBAPINetworkResponseCallback;
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.Endpoint;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONObject;
import org.junit.Test;

// TODO Several tests here are commented out, is this temporary or should they be deleted?
public class CBRequestManagerTest extends BaseTest {

    private EventTracker eventTrackerMock = mock(EventTracker.class);

    static TestContainer webViewSingleResponseTestContainer(ResponseDescriptor response) {
        return TestContainerBuilder.defaultWebView()
                .withResponse(response)
                .build();
    }

    /*
        A successful cacheInterstitial call should call ChartboostDelegate.didCacheInterstitial
        and track.trackLoad (was trackAdGet)
     */
//    @Test
//    public void success_delegateCallback() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                .withResponses(ReferenceResponse.webviewV2InterstitialGetWithResultsAssetDescriptors)
//                .startSdkWithDelegate()) {
//
//            verify(tc.delegate).didInitialize();
//
//            final String location = "default";
//            when(tc.delegate.shouldRequestInterstitial(location)).thenReturn(true);
//
//            Chartboost.cacheInterstitial(location);
//            tc.runExceptNextDelegateCall();
//
//            verify(tc.delegate).shouldRequestInterstitial(location);
//            verify(tc.delegate, never()).didCacheInterstitial(anyString());
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didCacheInterstitial(location);
//            tc.assertNoUiRunnablesReady();
//            verify(tc.delegate, never()).didFailToLoadInterstitial(anyString(), any(CBImpressionError.class));
//        }
//    }

    /*
        successful operation should call callback.onSuccess with the response body
     */
    @Test
    public void success_sendRequestCallback() {
        final ResponseDescriptor response = ReferenceResponse.webviewV2InterstitialGetWithResults;

        try (TestContainer tc = webViewSingleResponseTestContainer(response)) {

            CBAPINetworkResponseCallback callback = mock(CBAPINetworkResponseCallback.class);
            CBWebViewRequest request = new CBWebViewRequest(
                    EndpointRepositoryBase.EndPoint.INTERSTITIAL_GET.getDefaultValue(),
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    callback,
                    eventTrackerMock
            );
            tc.networkService.submit(request);

            //Send the Interstitial get request
            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable();

            final JSONObject expected = response.asJSONObject();
            verify(callback, only()).onSuccess(same(request), argThat(equalsJSONObject(expected)));
        }
    }

    @Test
    public void testValidationFailureCallsDeliverError() {
        try (TestContainer tc = TestContainerBuilder.defaultWebView()
                .withNetworkService()
                .build()) {

            JSONObject invalidResponse = jsonObject(JKV("status", 500));
            tc.responses.add(Endpoint.webviewV2Prefetch.ok(invalidResponse));
            CBAPINetworkResponseCallback callback = mock(CBAPINetworkResponseCallback.class);
            CBWebViewRequest request = new CBWebViewRequest(
                    tc.sdkConfig.get().webviewPrefetchEndpoint,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    callback,
                    eventTrackerMock
            );
            JSONObject webAssetList = new JSONObject();
            request.appendBodyArgument(CBConstants.REQUEST_PARAM_ASSET_LIST, webAssetList);

            request.checkStatusInResponseBody = true;

            tc.networkService.submit(request);

            //POST Prefetch request to the endpoint
            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable(); // the /webview/v2/prefetch request

            verify(callback, only()).onFailure(
                    same(request),
                    argThat(isCBInternalError(Internal.HTTP_NOT_OK)));
        }
    }

    /*
        If the adserver responds 200 / { "status": 404 }, we should see the following:
            delegate.didFailToLoadInterstitial(location, NO_AD_FOUND)
            track ad-error message with NO_AD_FOUND
     */
//    @Test
//    public void failToLoad_noAdFound_delegateCallback() {
//        verifyFailToLoad_delegateCallback(
//                ReferenceResponse.noAdvertiserCampaigns(Endpoint.webviewV2InterstitialGet),
//                CBImpressionError.NO_AD_FOUND);
//    }

    /*
        If the adserver responds 200 / { "status": 404, "message": "No advertiser campaigns." },
        CBRequestManager should call the callback with an HTTP_NOT_FOUND error.

        This is at the sendRequest / callback level
     */
    @Test
    public void failToLoad_noAdFound_sendRequestCallback() {
        verifyFailToLoad_sendRequestCallback(
                ReferenceResponse.noAdvertiserCampaigns(Endpoint.webviewV2InterstitialGet),
                Internal.HTTP_NOT_FOUND);
    }

    /*
        Verify delegate callback and track output in a scenario where an impression
        fails to load.

        This is verifies expected behavior on the callback passed to CBRequestManager.sendRequest
     */
    private void verifyFailToLoad_sendRequestCallback(ResponseDescriptor response,
                                                      Internal expectedInternalError) {
        try (TestContainer tc = webViewSingleResponseTestContainer(response)) {

            CBAPINetworkResponseCallback callback = mock(CBAPINetworkResponseCallback.class);
            CBWebViewRequest request = new CBWebViewRequest(
                    EndpointRepositoryBase.EndPoint.INTERSTITIAL_GET.getDefaultValue(),
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    callback,
                    eventTrackerMock
            );
            request.checkStatusInResponseBody = true;
            tc.networkService.submit(request);

            //Request interstitial and process the response
            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable();

            verify(callback, only()).onFailure(
                    same(request),
                    argThat(isCBInternalError(expectedInternalError)));
        }
    }

    /*
        If the adserver responds 300, we should get a NETWORK_FAILURE.

        CBRequestManager.CustomRequest.parseServerResponse won't be called here,
        because the network dispatchers only call that for status codes [200, 300)

        Tested here at the sendRequest / callback level
     */
    @Test
    public void failToLoad_nonSuccess_internalError_sendRequestCallback() {
        ResponseDescriptor response = Endpoint.webviewV2InterstitialGet.respond().withNoData(300);

        verifyFailToLoad_sendRequestCallback(response, Internal.NETWORK_FAILURE);
    }

    /*
        If the adserver responds 500, we should get a NETWORK_FAILURE

        This is at the sendRequest/callback level
     */
    @Test
    public void failToLoad_internalError_sendRequestCallback() {
        ResponseDescriptor response = Endpoint.webviewV2InterstitialGet.respond().withInternalError();

        verifyFailToLoad_sendRequestCallback(response, Internal.NETWORK_FAILURE);
    }

    /*
        If the adserver responds 200 but with a response body that isn't json, we should get an INTERNAL error
     */
//    @Test
//    public void failToLoad_badJson_delegateCallback() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withResponse(badJson(Endpoint.webviewV2InterstitialGet))
//                .startSdkWithDelegate()) {
//            when(tc.delegate.shouldRequestInterstitial("default")).thenReturn(true);
//            Chartboost.showInterstitial("default");
//
//            tc.runNextBackgroundRunnable();
//            tc.runNextNetworkRunnable();
//            tc.runNextBackgroundRunnable();
//
//            verify(tc.delegate, never()).didFailToLoadInterstitial(anyString(), any(CBImpressionError.class));
//            tc.runNextUiRunnable();
//            verify(tc.delegate).didFailToLoadInterstitial("default", CBImpressionError.INTERNAL);
//        }
//    }

    /*
        If the adserver responds 200 but with a response body that isn't json,
        then callback.onFailure should be called with error=MISCELLANEOUS
        (the JSON parse failure gets caught as a JSONException)
     */
    @Test
    public void failToLoad_badJson_sendRequestCallback() {
        try (TestContainer tc = webViewSingleResponseTestContainer(badJson(Endpoint.webviewV2InterstitialGet))) {

            CBAPINetworkResponseCallback callback = mock(CBAPINetworkResponseCallback.class);
            CBWebViewRequest request = new CBWebViewRequest(
                    EndpointRepositoryBase.EndPoint.INTERSTITIAL_GET.getDefaultValue(),
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    callback,
                    eventTrackerMock
            );

            tc.networkService.submit(request);

            //Request interstitial and process the response
            tc.runNextNetworkRunnable();
            tc.runNextUiRunnable();

            verify(callback, only()).onFailure(
                    same(request),
                    argThat(isCBInternalError(Internal.MISCELLANEOUS)));
        }
    }

    /*
        If the adserver responds 200 but with json that doesn't pass validation, then we should get
        an INTERNAL error.
     */
//    @Test
//    public void failToLoad_jsonFailsValidation_delegateCallback() {
//        verifyFailToLoad_delegateCallback(
//                ReferenceResponse.emptyJsonBody(Endpoint.webviewV2InterstitialGet),
//                CBImpressionError.INTERNAL);
//    }

    /*
        If CBReachability indicates the network is unavailable, we should get a
        failure callback with error=INTERNET_UNAVAILABLE
     */
//    @Test
//    public void failToLoad_networkUnavailableAtShow_delegateCallback() {
//        try (TestContainer tc = TestContainerBuilder.defaultWebView()
//                .withInterceptedMock(CBReachability.class)
//                .withResponse(ReferenceResponse.webviewV2InterstitialGetWithResults)
//                .startSdkWithDelegate()) {
//
//            when(tc.reachability.isNetworkAvailable()).thenReturn(false);
//
//            when(tc.delegate.shouldRequestInterstitial("default")).thenReturn(true);
//            Chartboost.showInterstitial("default");
//
//            verify(tc.delegate, never()).didFailToLoadInterstitial(anyString(), any(CBImpressionError.class));
//
//            tc.run();
//            verify(tc.delegate).didFailToLoadInterstitial("default", CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW);
//        }
//    }

    @Test
    public void parseServerResponse_success() {
        try (TestContainer tc = TestContainer.defaultWebView()) {
            CBNetworkServerResponse serverResponse = mockServerResponse(200, 200, "all good");
            CBNetworkRequestResult result = getParseServerResponse(tc, serverResponse);

            assertNotNull(result.value);
            assertNull(result.error);

            JSONObject json = (JSONObject) result.value;
            assertNotNull(json);

            assertThat((Integer) json.opt("status"), is(200));
            assertThat((String) json.opt("message"), is("all good"));
        }
    }

    @Test
    public void parseServerResponse_bad_json() {
        try (TestContainer tc = TestContainer.defaultWebView()) {
            CBNetworkServerResponse serverResponse = mockServerResponse(200, "not valid json");
            CBNetworkRequestResult result = getParseServerResponse(tc, serverResponse);

            assertNull(result.value);
            assertNotNull(result.error);

            CBError cbError = result.error;
            assertThat(cbError.getType(), is(Internal.MISCELLANEOUS));
            assertThat(cbError.getMessage(), is("Value not of type java.lang.String cannot be converted to JSONObject"));
        }
    }

    @Test
    public void parseServerResponse_404_not_found() {
        try (TestContainer tc = TestContainer.defaultWebView()) {
            CBNetworkServerResponse serverResponse = mockServerResponse(200, 404, "not found");
            CBNetworkRequestResult result = getParseServerResponse(tc, serverResponse);
            assertNull(result.value);
            CBError cbError = result.error;
            assertThat(cbError.getType(), is(Internal.HTTP_NOT_FOUND));
            assertThat(cbError.getMessage(), is("{\"status\":404,\"message\":\"not found\"}"));
        }
    }

    private CBNetworkRequestResult getParseServerResponse(TestContainer tc, CBNetworkServerResponse serverResponse) {
        CBRequest request = new CBRequest(
                CBConstants.API_ENDPOINT,
                "a path",
                tc.requestBodyBuilder.build(),
                Priority.NORMAL,
                null,
                eventTrackerMock
        );
        request.checkStatusInResponseBody = true;

        return request.parseServerResponse(serverResponse);
    }

    @NonNull
    private CBNetworkServerResponse mockServerResponse(int httpStatusCode, int bodyStatusCode, String bodyMessage) {
        JSONObject body = jsonObject(
                JKV("status", bodyStatusCode),
                JKV("message", bodyMessage));

        return mockServerResponse(httpStatusCode, body.toString());
    }

    @NonNull
    private CBNetworkServerResponse mockServerResponse(int httpStatusCode, String bodyString) {
        return new CBNetworkServerResponse(httpStatusCode, bodyString.getBytes());
    }
}
