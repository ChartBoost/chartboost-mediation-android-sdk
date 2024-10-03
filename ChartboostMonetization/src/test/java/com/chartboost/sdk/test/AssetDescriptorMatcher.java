package com.chartboost.sdk.test;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class AssetDescriptorMatcher {
    public static Matcher<AssetDescriptor> hasUri(final String uri) {
        return new FeatureMatcher<AssetDescriptor, String>(equalTo(uri), "uri", "uri") {
            @Override
            protected String featureValueOf(AssetDescriptor actual) {
                return actual.uri;
            }
        };
    }

}
