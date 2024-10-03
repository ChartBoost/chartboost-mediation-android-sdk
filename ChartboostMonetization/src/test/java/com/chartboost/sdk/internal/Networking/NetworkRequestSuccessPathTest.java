package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Libraries.CBJSON.JKV;
import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;
import static org.mockito.Mockito.mock;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.Endpoint;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestContainerBuilder;
import com.chartboost.sdk.tracking.EventTracker;

import org.json.JSONObject;
import org.junit.Test;

public class NetworkRequestSuccessPathTest extends BaseTest {
    /*
        Make sure the track event looks right in the success case.
     */
    @Test
    public void mock_sendToSessionLogs() {
        try (TestContainer tc = TestContainerBuilder.defaultWebView()
                .withNetworkService()
                .build()) {
            JSONObject response = jsonObject(JKV("a", 4), JKV("b", 6));

            EventTracker eventTrackerMock = mock(EventTracker.class);
            tc.responses.add(Endpoint.apiInstall.ok(response));
            CBRequest.CBAPINetworkResponseCallback cb = mock(CBRequest.CBAPINetworkResponseCallback.class);
            CBRequest req = new CBRequest(
                    CBConstants.API_ENDPOINT,
                    "api/install",
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    cb,
                    eventTrackerMock
            );
            tc.networkService.submit(req);
            tc.run();

            // TODO Not checked?
            JSONObject expectedMeta = jsonObject(
                    JKV("endpoint", "/api/install"),
                    JKV("statuscode", 200),
                    JKV("error", "None"),
                    JKV("errorDescription", "None"),
                    JKV("retryCount", 0));
        }
    }

}
