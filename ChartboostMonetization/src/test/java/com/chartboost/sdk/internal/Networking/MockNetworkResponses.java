package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.ResponseDescriptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockNetworkResponses {
    private final Map<String, ResponseDescriptor> responses = new HashMap<>();

    public MockNetworkResponses() {
    }

    ResponseDescriptor findResponse(CBNetworkRequest<?> request) {
        String key = getResponseKey(request.getMethod(), request.getUri());
        return responses.get(key);
    }

    public void add(ResponseDescriptor response) {
        String key = getResponseKey(response.endpoint.method, response.endpoint.uri);
        responses.put(key, response);
    }

    public void add(AssetDescriptor descriptor) {
        add(descriptor.respond());
    }

    public void add(List<AssetDescriptor> descriptors) {
        for (AssetDescriptor descriptor : descriptors) {
            add(descriptor);
        }
    }

    public void add(AssetDescriptor[] descriptors) {
        add(Arrays.asList(descriptors));
    }

    private String getResponseKey(CBNetworkRequest.Method method, String uri) {
        return method + " " + uri;
    }
}
