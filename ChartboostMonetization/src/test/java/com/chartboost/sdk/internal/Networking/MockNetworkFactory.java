package com.chartboost.sdk.internal.Networking;

import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chartboost.sdk.internal.Networking.CBNetworkRequest.Method;
import com.chartboost.sdk.test.Endpoint;
import com.chartboost.sdk.test.ResponseDescriptor;
import com.chartboost.sdk.test.TestTimeSource;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class MockNetworkFactory extends NetworkFactory {
    private final TestTimeSource testTimeSource;
    private final MockNetworkResponses responses;
    private final Map<String, List<HttpsURLConnection>> returnedConnections = new HashMap<>();

    public MockNetworkFactory(TestTimeSource testTimeSource, MockNetworkResponses responses) {
        this.responses = responses;
        this.testTimeSource = testTimeSource;
    }

    public HttpsURLConnection getMockConnectionReturnedForRequest(Method method, String uri) {
        List<HttpsURLConnection> allReturned = returnedConnections.get(getRequestKey(method, uri));
        if (allReturned.size() == 1)
            return allReturned.get(0);
        else
            throw new Error("Multiple connections were returned for " + method + " / " + uri);
    }

    @Override
    public HttpsURLConnection openConnection(CBNetworkRequest<?> request) throws IOException {
        final HttpsURLConnection conn = mock(HttpsURLConnection.class, RETURNS_SMART_NULLS);

        ResponseDescriptor foundResponse = responses.findResponse(request);
        final ResponseDescriptor response = (foundResponse != null)
                ? foundResponse
                : new Endpoint(request.getMethod(), request.getUri()).notFound();

        Answer<Integer> getResponseCodeAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                if (response.getResponseCodeDelayMs != 0) {
                    testTimeSource.advanceUptime(response.getResponseCodeDelayMs, TimeUnit.MILLISECONDS);
                }
                if (response.getResponseCodeIOException != null) {
                    throw response.getResponseCodeIOException;
                }
                return response.statusCode;
            }
        };
        when(conn.getResponseCode()).thenAnswer(getResponseCodeAnswer);

        if (response.statusCode == HttpsURLConnection.HTTP_NOT_FOUND) {
            when(conn.getInputStream()).thenThrow(new FileNotFoundException("uri " + request.getUri() + " not found"));
            when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream(response.data));
        } else if (response.statusCode >= 400) {
            when(conn.getInputStream()).thenThrow(new IOException("Server returned HTTP response code: " + response.statusCode + " for URL: " + request.getUri()));
            when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream(response.data));
        } else {
            Answer<InputStream> getInputStreamAnswer = new Answer<InputStream>() {

                @Override
                public InputStream answer(InvocationOnMock invocation) throws Throwable {
                    if (response.readDataMs > 0)
                        testTimeSource.advanceUptime(response.readDataMs, TimeUnit.MILLISECONDS);
                    return new ByteArrayInputStream(response.data);
                }
            };
            when(conn.getInputStream()).thenAnswer(getInputStreamAnswer);
        }

        if (response.getOutputStreamIOException != null) {
            when(conn.getOutputStream()).thenThrow(response.getOutputStreamIOException);
        } else {
            when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        }

        String requestKey = getRequestKey(request.getMethod(), request.getUri());
        List<HttpsURLConnection> conns = returnedConnections.get(requestKey);
        if (conns == null) {
            conns = new ArrayList<>();
            returnedConnections.put(requestKey, conns);
        }
        conns.add(conn);

        return conn;
    }

    private String getRequestKey(Method method, String uri) {
        return method + " " + uri;
    }

}
