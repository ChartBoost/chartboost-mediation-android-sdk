package com.chartboost.sdk.internal.AssetLoader;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

class AssetInfoMatcher {
    static Matcher<AssetInfo> assetInfoWithUri(final String uri) {
        return new FeatureMatcher<AssetInfo, String>(equalTo(uri), "asset info uri", "asset info uri") {
            @Override
            protected String featureValueOf(AssetInfo actual) {
                return actual.uri;
            }
        };
    }
}
