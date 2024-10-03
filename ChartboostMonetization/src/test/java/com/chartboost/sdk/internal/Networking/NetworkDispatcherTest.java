package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Model.CBError.Internal.NETWORK_FAILURE;
import static com.chartboost.sdk.internal.Model.CBErrorMatcher.hasError;
import static com.chartboost.sdk.internal.Networking.CBNetworkServerResponseMatcher.matchesResponse;
import static com.chartboost.sdk.test.TestUtils.assertByteArrayMatchesResource;
import static com.chartboost.sdk.test.TestUtils.readResourceToByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Priority;
import com.chartboost.sdk.test.ReferenceResponse;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestContainer;
import com.chartboost.sdk.test.TestUtils;
import com.chartboost.sdk.tracking.EventTracker;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class NetworkDispatcherTest extends BaseTest {

    private final EventTracker eventTrackerMock = mock(EventTracker.class);

    @Test
    public void testCompareTo() {
        try (TestContainer tc = new TestContainer()) {
            CBNetworkRequest requestHigh = mock(CBNetworkRequest.class);
            when(requestHigh.getPriority()).thenReturn(Priority.HIGH);
            CBNetworkRequest requestLow = mock(CBNetworkRequest.class);
            when(requestLow.getPriority()).thenReturn(Priority.LOW);
            final NetworkFactory factory = mock(NetworkFactory.class);
            NetworkDispatcher networkDispatcherHigh = new NetworkDispatcher(
                    tc.backgroundExecutor,
                    factory,
                    tc.reachability,
                    tc.timeSource,
                    tc.uiPoster,
                    requestHigh,
                    eventTrackerMock
            );
            NetworkDispatcher networkDispatcherLow = new NetworkDispatcher(
                    tc.backgroundExecutor,
                    factory,
                    tc.reachability,
                    tc.timeSource,
                    tc.uiPoster,
                    requestLow,
                    eventTrackerMock
            );

            assertThat(networkDispatcherHigh.compareTo(networkDispatcherLow), lessThan(0));
        }
    }

    @Test
    public void testHappyPath() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            String resourcePath = "com/chartboost/webview/v2/reward/get/response_with_results.json";
            when(setup.connection.getInputStream()).thenReturn(new ByteArrayInputStream(readResourceToByteArray(resourcePath)));

            setup.submitAndProcessRequest();
            SingleRequestSetup.TransmittedObject ob = setup.captureDeliveredResponse();

            assertThat(ob.statusCode, is(HttpsURLConnection.HTTP_OK));
            assertByteArrayMatchesResource(ob.data, resourcePath);
        }
    }

    /*
        It should parse the server response in the background
     */
    @Test
    public void testParsesServerResponseInBackground() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            ResponseDescriptor reference = ReferenceResponse.webviewV2RewardGetWithResults;
            when(setup.connection.getInputStream()).thenReturn(new ByteArrayInputStream(reference.copyBytes()));

            setup.submitRequest();

            setup.tc.run();

            verify(setup.request).parseServerResponse(argThat(matchesResponse(reference.asServerResponse())));
        }
    }

    /*
    If the response code indicates there's no data, it should not read the input stream.
    */
    @Test
    public void testProcessRequestNoData() throws Exception {
        // 1xx, 204, 304 return no data.  See https://tools.ietf.org/html/rfc7230#section-3.3
        List<Integer> noDataResponseCodes = Arrays.asList(100, 150, 199, 204, 304);
        for (int responseCode : noDataResponseCodes) {
            try (SingleRequestSetup setup = new SingleRequestSetup()) {
                when(setup.connection.getInputStream()).thenThrow(new Error("should not have requested input stream"));
                when(setup.connection.getResponseCode()).thenReturn(responseCode);
                setup.submitAndProcessRequest();

                if (responseCode >= 200 && responseCode <= 299) {
                    SingleRequestSetup.TransmittedObject ob = setup.captureDeliveredResponse();
                    assertThat(ob.statusCode, is(responseCode));
                    assertThat(ob.data, is(new byte[0]));
                } else {
                    // server responses outside 200..299 deliver an error
                    setup.verifyErrorDelivery(NETWORK_FAILURE, "Failure due to HTTP status code " + responseCode);
                }
            }
        }
    }

    /*
     If connection.getInputStream throws, then it should back off to connection.getErrorStream
    */
    @Test
    public void testBackoffToErrorStream() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            String resourcePath = "com/chartboost/webview/v2/reward/get/response_with_results.json";
            when(setup.connection.getInputStream()).thenThrow(new IOException("simulated"));
            when(setup.connection.getErrorStream()).thenReturn(new ByteArrayInputStream(readResourceToByteArray(resourcePath)));

            setup.submitAndProcessRequest();

            SingleRequestSetup.TransmittedObject ob = setup.captureDeliveredResponse();
            assertThat(ob.statusCode, is(HttpsURLConnection.HTTP_OK));
            assertByteArrayMatchesResource(ob.data, resourcePath);
        }
    }

    /*
        Status Code 401/403 should deliver an AuthFailureException
     */
    @Test
    public void testAuthorizationFailures() throws Exception {
        int[] statusCodes = {401, 403};
        for (int statusCode : statusCodes) {
            try (SingleRequestSetup setup = new SingleRequestSetup()) {

                when(setup.connection.getResponseCode()).thenReturn(statusCode);
                String resourcePath = "com/chartboost/webview/v2/reward/get/response_with_results.json";
                when(setup.connection.getInputStream()).thenReturn(new ByteArrayInputStream(readResourceToByteArray(resourcePath)));

                setup.submitAndProcessRequest();

                setup.verifyErrorDelivery(NETWORK_FAILURE, "Failure due to HTTP status code " + statusCode);
            }
        }
    }

    /*
     Response codes outside of 200..299, other than 401 and 403, should deliver a ServerException
    */
    @Test
    public void testServerErrors() throws Exception {
        int[] statusCodes = {100, 199, 300, 306, 400, 402, 502};
        for (int statusCode : statusCodes) {
            try (SingleRequestSetup setup = new SingleRequestSetup()) {

                String resourcePath = "com/chartboost/webview/v2/reward/get/response_with_results.json";
                when(setup.connection.getResponseCode()).thenReturn(statusCode);
                when(setup.connection.getInputStream()).thenReturn(new ByteArrayInputStream(readResourceToByteArray(resourcePath)));

                setup.submitAndProcessRequest();

                // it's going to deliver a server error
                setup.verifyErrorDelivery(NETWORK_FAILURE, "Failure due to HTTP status code " + statusCode);

                verify(setup.request, never()).parseServerResponse(any(CBNetworkServerResponse.class));
                verify(setup.request, never()).deliverResponse(any(SingleRequestSetup.TransmittedObject.class), any(CBNetworkServerResponse.class));
            }
        }
    }

    /*
    CBRequestManager.deliverError calls deliverResponse, rather than callback.onFailure(),
       in this scenario:
         1. parseServerResponse returns CBNetworkRequestResult.failure
         2. the CBNetworkRequestError passed to deliverError has serverResponse != null
         3. the status code of that serverResponse is 200
    */
    @Test
    public void testServerParseFailure() throws Exception {
        int statusCode = 200;
        try (SingleRequestSetup setup = SingleRequestSetup.postWithFailedParse()) {
            byte[] data = ReferenceResponse.webviewV2RewardGetWithResults.copyBytes();
            when(setup.connection.getResponseCode()).thenReturn(statusCode);
            when(setup.connection.getInputStream()).thenReturn(new ByteArrayInputStream(data));

            setup.submitAndProcessRequest();

            CBNetworkServerResponse expectedResponse = new CBNetworkServerResponse(statusCode, data);

            verify(setup.request).parseServerResponse(argThat(matchesResponse(expectedResponse)));
            verify(setup.request).deliverError(
                    argThat(hasError(NETWORK_FAILURE)), argThat(matchesResponse(expectedResponse))
            );
            verify(setup.request, never()).deliverResponse(any(SingleRequestSetup.TransmittedObject.class), any(CBNetworkServerResponse.class));
        }
    }

    /*
    If connection.getInputStream returns null, the data should be an empty byte array.
    */
    @Test
    public void testReturnEmptyByteArrayForNullInputStream() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            when(setup.connection.getInputStream()).thenReturn(null);

            setup.submitAndProcessRequest();

            SingleRequestSetup.TransmittedObject ob = setup.captureDeliveredResponse();
            assertThat(ob.data, is(new byte[0]));
        }
    }

    /*
        It should neither parse nor deliver messages that have been canceled.
    */
    @Test
    public void testSkipCanceledRequests() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            setup.request.cancel();
            setup.submitRequest();

            setup.tc.runNextNetworkRunnable();

            // things the dispatcher might have done if it processed a canceled request:

            // it could have parsed the server response
            verify(setup.request, never()).parseServerResponse(any(CBNetworkServerResponse.class));

            // it could have delivered the responses on the ui thread
            verify(setup.request, never()).deliverResponse(any(SingleRequestSetup.TransmittedObject.class), any(CBNetworkServerResponse.class));

            // it could have posted something to be done on the UI thread
            setup.tc.verifyNoMoreRunnables();
        }
    }

    /*
        It should set a connect timeout of 10000 ms.
     */
    @Test
    public void testInitialTimeout() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            setup.submitAndProcessRequest();

            verify(setup.connection).setConnectTimeout(10000);
        }
    }

    /*
    It should retry once after a SocketTimeoutException, doubling the connect timeout.
    */
    @Test
    public void testConnectTimeoutDoublesOnRetry() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.POST, 2)) {
            when(setup.mockConnections[0].connectionInputStream.read((byte[]) any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));
            when(setup.mockConnections[1].connectionInputStream.read((byte[]) any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));

            setup.submitAndProcessRequest();

            InOrder inOrder = inOrder(setup.mockConnections[0].connection, setup.mockConnections[1].connection);
            inOrder.verify(setup.mockConnections[0].connection).setConnectTimeout(10000);
            inOrder.verify(setup.mockConnections[1].connection).setConnectTimeout(20000);
        }
    }

    /*
        It should retry SocketTimeoutExceptions but not other IOExceptions.
     */
    @Test
    public void testDoNotRetryNonTimeoutIOExceptions() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.POST, 1)) {
            ArgumentCaptor<CBError> errorCaptor = ArgumentCaptor.forClass(CBError.class);
            ArgumentCaptor<CBNetworkServerResponse> networkServerResponseCaptor = ArgumentCaptor.forClass(CBNetworkServerResponse.class);

            when(setup.mockConnections[0].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("simulated"));

            setup.submitAndProcessRequest();

            verify(setup.request).deliverError(errorCaptor.capture(), networkServerResponseCaptor.capture());
            assertNotNull(errorCaptor.getValue());
            assertNull(networkServerResponseCaptor.getValue());
        }
    }

    /*
        It should deliver a SocketTimeoutException if reading times out
    */
    @Test
    public void testDeliverTimeoutException() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.POST, 2)) {
            when(setup.mockConnections[0].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));
            when(setup.mockConnections[1].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));

            setup.submitAndProcessRequest();

            final CBError.Internal errorCode = NETWORK_FAILURE;
            final String errorDesc = "java.net.SocketTimeoutException: simulated";
            setup.verifyErrorDelivery(errorCode, errorDesc);
        }
    }

    /*
         It should deliver a NetworkException on non-timeout IOExceptions.
     */
    @Test
    public void testDeliverNetworkExceptionOnNonTimeoutIOException() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.POST, 2)) {
            when(setup.mockConnections[0].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("simulated"));
            when(setup.mockConnections[1].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("simulated"));

            setup.submitAndProcessRequest();

            setup.verifyErrorDelivery(NETWORK_FAILURE, "java.io.IOException: simulated");
        }
    }

    /*
       It should retry once, doubling the read timeout.
    */
    @Test
    public void testReadTimeoutDoublesOnRetry() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.POST, 2)) {
            when(setup.mockConnections[0].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));
            when(setup.mockConnections[1].connectionInputStream.read(any(), anyInt(), anyInt())).thenThrow(new SocketTimeoutException("simulated"));

            setup.submitAndProcessRequest();

            InOrder inOrder = inOrder(setup.mockConnections[0].connection, setup.mockConnections[1].connection);
            inOrder.verify(setup.mockConnections[0].connection).setReadTimeout(10000);
            inOrder.verify(setup.mockConnections[1].connection).setReadTimeout(20000);
        }
    }

    /*
        It should set headers on the connection
     */
    @Test
    public void testSetHeaders() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            setup.requestHeaders.put("a-header-key", "a-header-value");
            setup.requestHeaders.put("b-header-key", "b-header-value");

            setup.submitAndProcessRequest();

            verify(setup.connection).addRequestProperty("a-header-key", "a-header-value");
            verify(setup.connection).addRequestProperty("b-header-key", "b-header-value");
        }
    }

    /*
        It should deliver an error for a MalformedUrlException
    */
    @Test
    public void testDeliverErrorOnMalformedUrl() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            when(setup.factory.openConnection(setup.request)).thenThrow(new MalformedURLException(setup.uri));

            setup.submitAndProcessRequest();

            setup.verifyErrorDelivery(NETWORK_FAILURE, "java.net.MalformedURLException: " + setup.uri);
        }
    }

    /*
        It should set the body for method POST
     */
    @Test
    public void testSetPostData() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {

            String resourcePath = "com/chartboost/webview/v2/interstitial/get/response_with_results.json";
            byte[] body = TestUtils.readResourceToByteArray(resourcePath);
            when(setup.request.buildRequestInfo()).thenReturn(new CBNetworkRequestInfo(null, body, null));

            ByteArrayOutputStream capturedOutputStream = new ByteArrayOutputStream();
            when(setup.connection.getOutputStream()).thenReturn(capturedOutputStream);

            setup.submitAndProcessRequest();

            byte[] capturedOutput = capturedOutputStream.toByteArray();

            assertByteArrayMatchesResource(capturedOutput, resourcePath);
        }
    }

    /*
        It should set the content type when submitting POST data
     */
    @Test
    public void testSetPostDataContentType() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            final String contentType = "a-content-type";

            byte[] body = ReferenceResponse.webviewV2PrefetchWithResults.copyBytes();
            when(setup.request.buildRequestInfo()).thenReturn(new CBNetworkRequestInfo(null, body, contentType));

            setup.submitAndProcessRequest();

            verify(setup.connection).addRequestProperty(eq("Content-Type"), same(contentType));
        }
    }

    /*
        It should call connection.setDoOutput(true) when there is POST data
    */
    @Test
    public void testSetPostDoOutputTrue() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            final String contentType = "a-content-type";

            byte[] body = ReferenceResponse.webviewV2PrefetchWithResults.copyBytes();
            when(setup.request.buildRequestInfo()).thenReturn(new CBNetworkRequestInfo(null, body, contentType));

            setup.submitAndProcessRequest();

            verify(setup.connection).setDoOutput(true);
        }
    }

    /*
        For a POST with getBody()==null, there are a few things that should not happen.
     */
    @Test
    public void testPostNullData() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup()) {
            final String contentType = "a-content-type";
            final Map<String, String> noHeaders = Collections.emptyMap();


            when(setup.request.buildRequestInfo()).thenReturn(new CBNetworkRequestInfo(noHeaders, null, contentType));

            setup.submitAndProcessRequest();

            verify(setup.connection, never()).setDoOutput(true);
            verify(setup.connection, never()).addRequestProperty(eq("Content-Type"), anyString());
            verify(setup.connection, never()).getOutputStream();
        }
    }

    /*
        For a GET, post data should not be retrieved or set.
     */
    @Test
    public void testDoNotSetPostDataForGet() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.GET, 1)) {
            byte[] body = ReferenceResponse.webviewV2InterstitialGetWithResults.copyBytes();
            when(setup.request.buildRequestInfo()).thenReturn(new CBNetworkRequestInfo(null, body, null));

            setup.submitAndProcessRequest();

            verify(setup.connection, never()).setDoOutput(true);
            verify(setup.connection, never()).addRequestProperty(eq("Content-Type"), anyString());
            verify(setup.connection, never()).getOutputStream();
        }
    }

    /*
        Should fail with a NetworkError if connection.getResponseCode() returns -1
     */
    @Test
    public void testResponseCodeNegativeOneIsError() throws Exception {
        try (SingleRequestSetup setup = new SingleRequestSetup(CBNetworkRequest.Method.GET, 1)) {
            when(setup.connection.getResponseCode()).thenReturn(-1);

            setup.submitAndProcessRequest();

            setup.verifyErrorDelivery(NETWORK_FAILURE, "java.io.IOException: Could not retrieve response code from HttpsURLConnection.");
        }
    }

    static class SingleRequestSetup implements AutoCloseable {
        public final String uri = "a uri";
        public final TestContainer tc = new TestContainer();

        public final CBNetworkService networkRequestService;
        public final NetworkFactory factory = mock(NetworkFactory.class);
        private final EventTracker eventTrackerMock = mock(EventTracker.class);

        @Override
        public void close() throws Exception {
            tc.close();
        }

        public class TransmittedObject {
            final int statusCode;
            final byte[] data;

            TransmittedObject(int statusCode, byte[] data) {
                this.statusCode = statusCode;
                this.data = Arrays.copyOf(data, data.length);
            }
        }

        public final Map<String, String> requestHeaders = new HashMap<>();

        public final CBNetworkRequest<TransmittedObject> request;

        public final MockConnection[] mockConnections;
        public final HttpsURLConnection connection; // for convenience, always mockConnections[0].connection

        public final NetworkDispatcher dispatcher;

        public static SingleRequestSetup postWithFailedParse() {
            return new SingleRequestSetup(CBNetworkRequest.Method.POST, 1, new CBError(NETWORK_FAILURE, "simulated failure"));
        }

        public SingleRequestSetup() {
            this(CBNetworkRequest.Method.POST, 1, null);
        }

        public SingleRequestSetup(CBNetworkRequest.Method method, int expectedConnectionAttempts) {
            this(method, expectedConnectionAttempts, null);
        }

        public SingleRequestSetup(CBNetworkRequest.Method method, int expectedConnectionAttempts, CBError parseFailure) {
            try {
                mockConnections = new MockConnection[expectedConnectionAttempts];
                for (int i = 0; i < expectedConnectionAttempts; i++) {
                    mockConnections[i] = new MockConnection();
                }

                connection = mockConnections[0].connection;

                request = spy(createRequest(method, parseFailure));
                dispatcher = new NetworkDispatcher(
                        tc.backgroundExecutor,
                        factory,
                        tc.reachability,
                        tc.timeSource,
                        tc.uiPoster,
                        request,
                        eventTrackerMock
                );
                networkRequestService = new CBNetworkService(
                        tc.backgroundExecutor,
                        factory,
                        tc.reachability,
                        tc.timeSource,
                        tc.uiPoster,
                        tc.networkExecutor,
                        eventTrackerMock
                );
                // I tried .thenAnswer(AdditionalAnswers.returnsElementsOf).thenThrow() but that
                // throws on the second call.
                if (expectedConnectionAttempts == 1) {
                    when(factory.openConnection(request))
                            .thenReturn(mockConnections[0].connection)
                            .thenThrow(new Error("did not expect more than one call to openConnection"));
                } else if (expectedConnectionAttempts == 2) {
                    when(factory.openConnection(request))
                            .thenReturn(
                                    mockConnections[0].connection,
                                    mockConnections[1].connection)
                            .thenThrow(new Error("did not expect more than two calls to openConnection"));

                }
            } catch (IOException ioe) {
                throw new Error(ioe);
            }
        }

        void submitRequest() {
            networkRequestService.submit(request);
        }

        void submitAndProcessRequest() {
            submitRequest();

            //POST a uri
            tc.runNextNetworkRunnable();
            // The dispatcher posts the result for execution on the UI thread:
            tc.runNextUiRunnable();
        }

        TransmittedObject captureDeliveredResponse() {
            ArgumentCaptor<TransmittedObject> responseCaptor = ArgumentCaptor.forClass(SingleRequestSetup.TransmittedObject.class);

            verify(request).deliverResponse(responseCaptor.capture(), any(CBNetworkServerResponse.class));

            verify(request, never()).deliverError(any(CBError.class), any(CBNetworkServerResponse.class));

            return responseCaptor.getValue();
        }

        void verifyErrorDelivery(CBError.Internal errorCode, String errorDesc) {
            ArgumentCaptor<CBError> argError = ArgumentCaptor.forClass(CBError.class);
            ArgumentCaptor<CBNetworkServerResponse> argResponse = ArgumentCaptor.forClass(CBNetworkServerResponse.class);

            verify(request).deliverError(argError.capture(), argResponse.capture());
            verify(request, never()).deliverResponse(any(TransmittedObject.class), any(CBNetworkServerResponse.class));
        }

        private CBNetworkRequest<TransmittedObject> createRequest(CBNetworkRequest.Method method, final CBError parseFailure) {
            return new CBNetworkRequest<TransmittedObject>(method, uri, Priority.NORMAL, null) {

                @Override
                public CBNetworkRequestResult<TransmittedObject> parseServerResponse(CBNetworkServerResponse serverResponse) {
                    if (parseFailure == null)
                        return CBNetworkRequestResult.success(new TransmittedObject(serverResponse.getStatusCode(), serverResponse.getData()));
                    else
                        return CBNetworkRequestResult.failure(parseFailure);
                }

                @Override
                public void deliverResponse(TransmittedObject response, CBNetworkServerResponse serverResponse) {

                }

                @Override
                public CBNetworkRequestInfo buildRequestInfo() {
                    return new CBNetworkRequestInfo(requestHeaders, null, null);
                }
            };
        }

        private class MockConnection {
            public final HttpsURLConnection connection = mock(HttpsURLConnection.class);

            public final InputStream connectionInputStream = mock(InputStream.class);
            public final InputStream connectionErrorStream = mock(InputStream.class);
            public final OutputStream connectionOutputStream = mock(OutputStream.class);

            MockConnection() {
                try {
                    when(connectionInputStream.read(any(), anyInt(), anyInt())).thenReturn(-1);
                    when(connectionInputStream.read()).thenReturn(-1);
                    when(connection.getResponseCode()).thenReturn(200);
                    when(connection.getInputStream()).thenReturn(connectionInputStream);
                    when(connection.getErrorStream()).thenReturn(connectionErrorStream);
                    when(connection.getOutputStream()).thenReturn(connectionOutputStream);
                } catch (IOException ioe) {
                    throw new Error(ioe);
                }
            }
        }

    }
}
