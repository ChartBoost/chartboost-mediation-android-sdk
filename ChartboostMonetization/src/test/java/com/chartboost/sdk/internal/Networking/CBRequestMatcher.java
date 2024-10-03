package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Libraries.CBJSON.walk;
import static com.chartboost.sdk.test.TestUtils.walkToAny;
import static com.chartboost.sdk.test.TestUtils.walkToArray;
import static org.hamcrest.Matchers.equalTo;

import com.chartboost.sdk.internal.Networking.requests.CBRequest;
import com.chartboost.sdk.test.JSONObjectMatcher;
import com.chartboost.sdk.test.TestUtils;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class CBRequestMatcher {
    public static Matcher<CBRequest> hasPath(final String uri) {
        return new FeatureMatcher<CBRequest, String>(equalTo(uri), "path", "path") {
            @Override
            protected String featureValueOf(CBRequest actual) {
                return actual.getPath();
            }
        };
    }

    public static Matcher<CBRequest> hasBodyValueWithName(final String name, final Object expected) {
        return new FeatureMatcher<CBRequest, Object>(equalTo(expected), name, name) {
            @Override
            protected Object featureValueOf(CBRequest actual) {
                try {
                    JSONObject body = new JSONObject(actual.body.toString());
                    return body.opt(name);
                } catch (JSONException ex) {
                    throw new Error(ex);
                }
            }
        };
    }

    public static Matcher<CBRequest> hasBodyValue(final Object expected, final String... path) {
        return new FeatureMatcher<CBRequest, Object>(equalTo(expected), "has json field " + TestUtils.join(path, "."), TestUtils.join(path, ".")) {
            @Override
            protected Object featureValueOf(CBRequest actual) {
                try {
                    JSONObject body = new JSONObject(actual.body.toString());
                    return walkToAny(body, path);
                } catch (JSONException ex) {
                    throw new Error(ex);
                }
            }
        };
    }

    public static Matcher<CBRequest> hasBodyValue(final JSONObject expected, final String... path) {
        return new TypeSafeDiagnosingMatcher<CBRequest>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }

            @Override
            protected boolean matchesSafely(CBRequest actualRequest, Description mismatchDescription) {
                JSONObject actual = walk(actualRequest.body, path);
                final String pathRoot = "";
                String diff = JSONObjectMatcher.jsonObjectDifference(pathRoot, actual, expected);
                mismatchDescription.appendValue(diff);
                return diff == null;
            }
        };
    }

    public static Matcher<CBRequest> hasBodyValue(final JSONArray expected, final String... path) {
        return new TypeSafeDiagnosingMatcher<CBRequest>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }

            @Override
            protected boolean matchesSafely(CBRequest actualRequest, Description mismatchDescription) {
                JSONArray actual = walkToArray(actualRequest.body, path);
                final String pathRoot = "";
                String diff = JSONObjectMatcher.jsonArrayDifference(pathRoot, actual, expected);
                mismatchDescription.appendValue(diff);
                return diff == null;
            }
        };
    }

    public static Matcher<CBRequest> hasNoBodyField(final String... path) {
        return new FeatureMatcher<CBRequest, Boolean>(equalTo(true),
                "does not have json field " + TestUtils.join(path, "."),
                TestUtils.join(path, ".")) {

            @Override
            protected Boolean featureValueOf(CBRequest actual) {
                String[] pathToParent = Arrays.copyOf(path, path.length - 1);

                JSONObject parent = walk(actual.body, pathToParent);
                return parent == null || !parent.has(path[path.length - 1]);
            }
        };
    }
}
