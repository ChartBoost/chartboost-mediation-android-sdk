package com.chartboost.sdk.internal.Model;

import com.chartboost.sdk.internal.Networking.CBNetworkRequest;
import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.internal.Priority;

public class NetworkParameters {

    public final CBNetworkRequest.Method method;
    public final String endpoint;
    public final String path;
    public final RequestBodyFields requestBodyFields;
    public final
    Priority priority;
    public final CBRequest.CBAPINetworkResponseCallback callback;

    public NetworkParameters(
            String endpoint,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            CBRequest.CBAPINetworkResponseCallback callback
    ) {
        this(CBNetworkRequest.Method.POST, endpoint, path, requestBodyFields, priority, callback);
    }

    public NetworkParameters(
            CBNetworkRequest.Method method,
            String endpoint,
            String path,
            RequestBodyFields requestBodyFields,
            Priority priority,
            CBRequest.CBAPINetworkResponseCallback callback
    ) {
        this.method = method;
        this.endpoint = endpoint;
        this.path = path;
        this.requestBodyFields = requestBodyFields;
        this.priority = priority;
        this.callback = callback;
    }
}
