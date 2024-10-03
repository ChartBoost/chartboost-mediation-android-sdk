package com.chartboost.sdk.internal.Networking;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Arrays;

public class CBNetworkServerResponseMatcher {
    public static Matcher<CBNetworkServerResponse> matchesResponse(final CBNetworkServerResponse expected) {
        return new TypeSafeDiagnosingMatcher<CBNetworkServerResponse>() {
            @Override
            protected boolean matchesSafely(CBNetworkServerResponse actual, Description mismatchDescription) {
                if (expected.getStatusCode() != actual.getStatusCode()) {
                    mismatchDescription.appendText("Status Code");
                    mismatchDescription.appendText("Expected");
                    mismatchDescription.appendValue(expected.getStatusCode());
                    mismatchDescription.appendText("Actual");
                    mismatchDescription.appendValue(actual.getStatusCode());
                    return false;
                }
                if (!Arrays.equals(expected.getData(), actual.getData())) {
                    mismatchDescription.appendText("data does not match");
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Status Code");
                description.appendValue(expected.getStatusCode());

            }
        };
    }
}
