package com.chartboost.sdk.test;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerMatcher {
    public static Matcher<AtomicInteger> hasValue(int expectedValue) {
        return new FeatureMatcher<AtomicInteger, Integer>(equalTo(expectedValue), "atomic integer value", "atomic integer value") {
            @Override
            protected Integer featureValueOf(AtomicInteger actual) {
                return actual.get();
            }
        };
    }

}
