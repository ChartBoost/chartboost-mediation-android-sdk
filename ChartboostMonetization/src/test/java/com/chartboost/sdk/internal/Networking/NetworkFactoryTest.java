package com.chartboost.sdk.internal.Networking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.NonNull;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.internal.Priority;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class NetworkFactoryTest extends BaseTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testOpenConnection() throws IOException {
        NetworkFactory factory = new NetworkFactory();

        String uri = "https://localhost:8080/a";
        CBNetworkRequest<?> request = getCbNetworkRequest(uri);
        HttpsURLConnection conn = factory.openConnection(request);
        URL url = conn.getURL();
        assertThat(url.getPath(), is("/a"));
    }

    @Test
    public void testOpenConnectionThrowsMalformedURLException() throws IOException {
        NetworkFactory factory = new NetworkFactory();


        String uri = "a bad uri";
        CBNetworkRequest<?> request = getCbNetworkRequest(uri);

        exception.expect(MalformedURLException.class);
        factory.openConnection(request);
    }

    @NonNull
    CBNetworkRequest<?> getCbNetworkRequest(final String uri) {
        return new CBNetworkRequest(CBNetworkRequest.Method.POST, uri, Priority.NORMAL, null) {
            @Override
            public CBNetworkRequestResult parseServerResponse(CBNetworkServerResponse serverResponse) {
                return null;
            }

            @Override
            public void deliverResponse(Object response, CBNetworkServerResponse serverResponse) {

            }
        };
    }
}
