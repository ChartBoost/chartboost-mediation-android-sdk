package com.chartboost.sdk.test;

import static com.chartboost.sdk.internal.Libraries.CBJSON.jsonObject;

import androidx.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class JSONObjectMatcher {
    public static Matcher<JSONObject> isEmptyJSONObject() {
        return equalsJSONObject(jsonObject());
    }

    public static Matcher<JSONObject> equalsJSONObject(final JSONObject expected) {
        return new TypeSafeDiagnosingMatcher<JSONObject>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }

            @Override
            protected boolean matchesSafely(JSONObject actual, Description mismatchDescription) {
                final String pathRoot = "";
                String diff = jsonObjectDifference(pathRoot, actual, expected);
                mismatchDescription.appendValue(diff);
                return diff == null;
            }
        };
    }

    public static String jsonObjectDifference(String path, JSONObject actual, JSONObject expected) {
        StringBuilder diffs = new StringBuilder();

        Set<String> actualKeys = getKeys(actual);
        Set<String> expectedKeys = getKeys(expected);

        Set<String> missingKeys = removeFromSet(expectedKeys, actualKeys);
        Set<String> extraKeys = removeFromSet(actualKeys, expectedKeys);

        for (String missingKey : missingKeys) {
            if (diffs.length() > 0)
                diffs.append("\n");
            diffs.append("is missing field: ");
            diffs.append(getChildPath(path, missingKey));
        }
        for (String extraKey : extraKeys) {
            if (diffs.length() > 0)
                diffs.append("\n");
            diffs.append("has unexpected field: ");
            diffs.append(getChildPath(path, extraKey));
            diffs.append("=");
            diffs.append(actual.opt(extraKey));
        }

        if (diffs.length() > 0)
            return diffs.toString();

        for (String key : actualKeys) {
            String childPath = getChildPath(path, key);

            String diff = elementDifference(childPath, actual.opt(key), expected.opt(key));
            if (diff != null) {
                return diff;
            }
        }

        return null;
    }

    @NonNull
    private static Set<String> removeFromSet(Set<String> source, Set<String> toRemove) {
        Set<String> remaining = new HashSet<>(source);
        remaining.removeAll(toRemove);
        return remaining;
    }

    @NonNull
    private static String getChildPath(String parentPath, String childName) {
        StringBuilder childPath = new StringBuilder();
        childPath.append(parentPath);
        if (!childName.contains(" ")) {
            if (!parentPath.isEmpty())
                childPath.append(".");
            childPath.append(childName);
        } else {
            childPath.append("[\"");
            childPath.append(childName);
            childPath.append("\"]");
        }
        return childPath.toString();
    }

    private static Set<String> getKeys(JSONObject obj) {
        Set<String> result = new HashSet<>();
        if (obj != null) {
            for (Iterator<String> itr = obj.keys(); itr.hasNext(); ) {
                result.add(itr.next());
            }
        }
        return result;
    }

    public static String jsonArrayDifference(String path, JSONArray actual, JSONArray expected) {
        if (actual.length() != expected.length())
            return String.format(Locale.US, "array length mismatch at %s: actual=%d expected=%d", path, actual.length(), expected.length());

        for (int i = 0; i < actual.length(); ++i) {
            String diff = elementDifference(path + "[" + i + "]", actual.opt(i), expected.opt(i));
            if (diff != null)
                return diff;
        }
        return null;
    }

    private static String elementDifference(String path, Object actual, Object expected) {
        String diff = null;

        if ((actual == null) != (expected == null)) {
            diff = String.format(Locale.US, "%s null mismatch: actual %s, expected %s",
                    path,
                    actual == null ? "is null" : "is not null",
                    expected == null ? "is null" : "is not null");
        } else if (actual instanceof JSONObject && expected instanceof JSONObject) {
            diff = jsonObjectDifference(path, (JSONObject) actual, (JSONObject) expected);
        } else if (actual instanceof JSONArray && expected instanceof JSONArray) {
            diff = jsonArrayDifference(path, (JSONArray) actual, (JSONArray) expected);
        } else if (actual != null && !actual.equals(expected)) {
            if (!matchingNumbers(actual, expected))
                diff = String.format(Locale.US, "mismatch at %s: actual=%s expected=%s", path, quotedIfString(actual), quotedIfString(expected));
        }

        return diff;
    }

    private static boolean matchingNumbers(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            Number actualNum = (Number) actual;
            Number expectedNum = (Number) expected;

            if (actual instanceof Double || expected instanceof Double) {
                return actualNum.doubleValue() == expectedNum.doubleValue();
            } else {
                return actualNum.longValue() == expectedNum.longValue();
            }
        }
        return false;
    }

    private static String quotedIfString(Object any) {
        String q = any instanceof String ? "\"" : "";
        return q + any + q;
    }
}
