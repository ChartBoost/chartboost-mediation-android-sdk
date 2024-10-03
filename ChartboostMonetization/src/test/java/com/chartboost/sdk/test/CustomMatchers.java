package com.chartboost.sdk.test;

import static org.hamcrest.Matchers.equalTo;

import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Model.CBError.Internal;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class CustomMatchers {

    public static Matcher<CBError> isCBInternalError(final Internal internalError) {
        return new FeatureMatcher<CBError, Internal>(equalTo(internalError), "internal error", "internal error") {
            @Override
            protected Internal featureValueOf(CBError actual) {
                return (Internal) actual.getType();
            }
        };
    }
}
