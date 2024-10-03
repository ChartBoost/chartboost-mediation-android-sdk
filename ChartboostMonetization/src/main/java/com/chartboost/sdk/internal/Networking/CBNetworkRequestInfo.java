package com.chartboost.sdk.internal.Networking;

import java.util.Map;

public class CBNetworkRequestInfo { // naming halp
    final Map<String, String> headers;
    final byte[] body;
    final String contentType;

    public CBNetworkRequestInfo(Map<String, String> headers, byte[] body, String contentType) {
        this.headers = headers;
        this.body = body;
        this.contentType = contentType;
    }
}
