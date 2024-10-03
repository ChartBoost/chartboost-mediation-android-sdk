package com.chartboost.sdk.internal.Model;

import static org.hamcrest.Matchers.equalTo;

import com.chartboost.sdk.test.AssetDescriptor;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class AssetMatcher {
    public static Matcher<Map<String, Asset>> mapValuesMatchAssetDescriptor(AssetDescriptor descriptor) {
        return mapValuesMatchAssetDescriptors(new AssetDescriptor[]{descriptor});
    }

    public static Matcher<Map<String, Asset>> mapValuesMatchAssetDescriptors(final AssetDescriptor[] descriptors) {
        return new FeatureMatcher<Map<String, Asset>, String>(equalTo(fromDescriptors(descriptors)), "assets", "assets") {

            @Override
            protected String featureValueOf(Map<String, Asset> actual) {
                return fromAssetMap(actual);
            }

        };
    }

    private static String fromDescriptors(AssetDescriptor[] expected) {
        List<String> x = new ArrayList<>();

        for (AssetDescriptor d : expected) {
            x.add(String.format("filename=%s directory=%s uri=%s\n", d.filename, d.cacheDir, d.uri));
        }
        Collections.sort(x);
        return x.toString();
    }

    private static String fromAssetMap(Map<String, Asset> actual) {
        List<String> x = new ArrayList<>();
        for (Asset asset : actual.values()) {
            x.add(String.format("filename=%s directory=%s uri=%s\n", asset.filename, asset.directory, asset.url));
        }
        Collections.sort(x);
        return x.toString();
    }
}
