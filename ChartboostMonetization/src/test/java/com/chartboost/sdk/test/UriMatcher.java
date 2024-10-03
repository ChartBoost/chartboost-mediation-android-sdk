package com.chartboost.sdk.test;

import static org.hamcrest.Matchers.equalTo;

import android.net.Uri;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class UriMatcher {
    public static Matcher<Uri> isUriForPath(String path) {
        return new FeatureMatcher<Uri, String>(equalTo(path), "uri", "uri") {
            @Override
            protected String featureValueOf(Uri actual) {
                // new Uri("http://localhost/a/b/c").getPath() returns "a/b/c"
                // The .toString of that returns "http://localhost/a/b/c"
                return actual.toString();
            }
        };
    }

}
