package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static com.chartboost.sdk.internal.Networking.CBNetworkRequest.API_ENDPOINT_CONFIG;
import static com.chartboost.sdk.internal.Networking.CBNetworkRequest.API_ENDPOINT_INTERSTITIAL_GET;
import static com.chartboost.sdk.internal.Networking.CBNetworkRequest.API_ENDPOINT_REWARD_GET;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.internal.verification.VerificationModeFactory.only;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Model.CBError.Internal;
import com.chartboost.sdk.internal.Model.CBErrorMatcher;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Networking.requests.CBRequest.CBAPINetworkResponseCallback;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.Endpoint;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

public class NetworkRequestErrorPathTest extends BaseTest {
    private final String INVALID_API_ENDPOINT = "invalid_api_endpoint";

    private final Endpoint invalidEndpoint = Endpoint.post(INVALID_API_ENDPOINT);

    private final EventTracker eventTrackerMock = mock(EventTracker.class);

    private TestContainer testContainer() {
        return TestContainerBuilder.defaultWebView()
                .withNetworkService()
                .build();
    }

    /*
        If the hostname is invalid, HttpsURLConnection.getOutputStream will throw an UnknownHostException.

        We expect callback.onFailure to be called.
     */
    @Test
    public void mock_badHostName() {
        try (TestContainer tc = testContainer()) {
            IOException simulatedException = new UnknownHostException("the_invalid_hostname");
            tc.responses.add(Endpoint.apiConfig.respond().withGetOutputStreamIOException(simulatedException));
            CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    API_ENDPOINT_CONFIG,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            tc.networkService.submit(req);
            tc.run();

            verify(cb, only()).onFailure(
                    same(req),
                    argThat(allOf(
                            CBErrorMatcher.hasError(Internal.NETWORK_FAILURE),
                            CBErrorMatcher.hasErrorDesc("java.net.UnknownHostException: the_invalid_hostname"))));
        }
    }

    @Test
    public void mock_badEndpoint() {
        try (TestContainer tc = testContainer()) {
            JSONObject responseBody = jsonObject(
                    JKV("status", 403),
                    JKV("message", "Access denied. Requires login!"));
            tc.responses.add(invalidEndpoint.ok(responseBody));

            CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    INVALID_API_ENDPOINT,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            req.checkStatusInResponseBody = true;
            tc.networkService.submit(req);
            tc.run();

            final String errorMessage = responseBody.toString();
            verify(cb, only()).onFailure(
                    same(req),
                    argThat(
                            allOf(
                                    CBErrorMatcher.hasError(Internal.HTTP_NOT_OK),
                                    CBErrorMatcher.hasErrorDesc(errorMessage)
                            )
                    )
            );
        }
    }

    @Test
    public void mock_badIdentity() {
        try (TestContainer tc = testContainer()) {

            final JSONObject responseBody = jsonObject(
                    JKV("message", "Bad identity. hexBinary needs to be even-length: the identifiers"),
                    JKV("status", 400));

            CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
            tc.responses.add(Endpoint.rewardGet.respond(400, responseBody));
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    API_ENDPOINT_REWARD_GET,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            req.checkStatusInResponseBody = true;
            tc.networkService.submit(req);
            tc.run();

            verify(cb, only()).onFailure(
                    same(req),
                    argThat(allOf(
                            CBErrorMatcher.hasError(Internal.NETWORK_FAILURE),
                            CBErrorMatcher.hasErrorDesc("Failure due to HTTP status code 400"))));
        }
    }

    /*
        If the server returns 500, HttpsURLConnection.getInputStream() will throw an IOException.

        The NetworkDispatcher falls back to getErrorStream().  We expect an onFailure callback,
        even if the response body matches the response mask.
     */
    @Test
    public void mock_endpointReturns500() {
        try (TestContainer tc = testContainer()) {

            final JSONObject responseBody = jsonObject(
                    JKV("status", 200));

            tc.responses.add(Endpoint.interstitialGet.respond(500, responseBody));
            CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    API_ENDPOINT_INTERSTITIAL_GET,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            req.checkStatusInResponseBody = true;
            tc.networkService.submit(req);
            tc.run();

            final String errorMessage = "Failure due to HTTP status code 500";

            verify(cb, only()).onFailure(
                    same(req),
                    argThat(allOf(
                            CBErrorMatcher.hasError(Internal.NETWORK_FAILURE),
                            CBErrorMatcher.hasErrorDesc(errorMessage))));
        }
    }

    @Test
    public void mock_authorizationFailure() {
        int[] statusCodes = {HttpsURLConnection.HTTP_UNAUTHORIZED, HttpsURLConnection.HTTP_FORBIDDEN};
        for (int statusCode : statusCodes) {

            try (TestContainer tc = testContainer()) {
                final JSONObject responseBody = jsonObject(
                        JKV("x", 5),
                        JKV("y", 7));

                tc.responses.add(Endpoint.rewardGet.respond(statusCode, responseBody));

                CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
                CBRequest req = new CBRequest(
                        CBConstants.API_ENDPOINT,
                        API_ENDPOINT_REWARD_GET,
                        tc.requestBodyBuilder.build(),
                        Priority.NORMAL,
                        cb,
                        eventTrackerMock
                );
                req.checkStatusInResponseBody = true;

                tc.networkService.submit(req);
                tc.run();

                verify(cb, only()).onFailure(
                        same(req),
                        argThat(allOf(
                                CBErrorMatcher.hasError(Internal.NETWORK_FAILURE),
                                CBErrorMatcher.hasErrorDesc("Failure due to HTTP status code " + statusCode))));
            }
        }
    }

    /*
        If the response fails validation, we expect to get an onFailure() callback
        even if the server response code is 200.

        CBRequest.deliverError has a clause that actually calls the onSuccess callback
        if there is a server response with status code 200.
     */
    @Test
    public void mock_failsValidation() {
        try (TestContainer tc = testContainer()) {
            final JSONObject responseBody = jsonObject(
                    JKV("x", 4),
                    JKV("y", 7)
            );

            tc.responses.add(Endpoint.rewardGet.respond(200, responseBody));

            CBAPINetworkResponseCallback cb = mock(CBAPINetworkResponseCallback.class);
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    API_ENDPOINT_REWARD_GET,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            req.checkStatusInResponseBody = true;

            tc.networkService.submit(req);
            tc.run();

            verify(cb, only()).onFailure(
                    same(req),
                    argThat(
                            allOf(
                                    CBErrorMatcher.hasError(Internal.HTTP_NOT_OK),
                                    CBErrorMatcher.hasErrorDesc("{\"status\":0,\"message\":\"\"}")
                            )
                    )
            );
        }
    }
}
