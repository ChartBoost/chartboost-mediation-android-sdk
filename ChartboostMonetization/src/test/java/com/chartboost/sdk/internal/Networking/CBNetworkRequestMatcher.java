package com.chartboost.sdk.internal.Networking;

import static org.hamcrest.Matchers.equalTo;

import com.chartboost.sdk.test.AssetDescriptor;
import com.chartboost.sdk.test.Endpoint;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class CBNetworkRequestMatcher {

    public static Matcher<CBNetworkRequest<?>> hasEndpointUri(Endpoint endpoint) {
        return hasUri(endpoint.uri);
    }

    public static Matcher<CBNetworkRequest<?>> matchesAssetDescriptor(AssetDescriptor d) {
        return hasUri(d.uri);
    }

    public static Matcher<CBNetworkRequest<?>> hasUri(String uri) {
        return new FeatureMatcher<CBNetworkRequest<?>, String>(equalTo(uri), "uri", "uri") {

            @Override
            protected String featureValueOf(CBNetworkRequest<?> actual) {
                return actual.getUri();
            }
        };
    }
}
