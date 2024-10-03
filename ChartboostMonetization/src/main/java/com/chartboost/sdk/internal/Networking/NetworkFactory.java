package com.chartboost.sdk.internal.Networking;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class NetworkFactory {
    HttpsURLConnection openConnection(CBNetworkRequest<?> request) throws IOException {
        return (HttpsURLConnection) new URL(request.getUri()).openConnection();
    }
}
