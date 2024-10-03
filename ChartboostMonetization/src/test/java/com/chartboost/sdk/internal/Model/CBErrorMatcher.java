package com.chartboost.sdk.internal.Model;

import static org.hamcrest.Matchers.equalTo;

import com.chartboost.sdk.internal.Model.CBError.Internal;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class CBErrorMatcher {
    public static Matcher<CBError> hasError(final Internal error) {
        return new FeatureMatcher<CBError, Internal>(equalTo(error), "internal error", "internal error") {
            @Override
            protected Internal featureValueOf(CBError actual) {
                return (Internal) actual.getType();
            }
        };
    }

    public static Matcher<CBError> hasErrorDesc(final String desc) {
        return new FeatureMatcher<CBError, String>(equalTo(desc), "error description", "error description") {
            @Override
            protected String featureValueOf(CBError actual) {
                return actual.getErrorDesc();
            }
        };
    }

}
