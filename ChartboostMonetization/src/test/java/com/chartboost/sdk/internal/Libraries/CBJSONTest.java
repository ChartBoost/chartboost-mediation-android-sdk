package com.chartboost.sdk.internal.Libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNull;

import com.chartboost.sdk.PlayServices.BaseTest;
import com.chartboost.sdk.test.TestContainer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class CBJSONTest extends BaseTest {

    @Test
    public void walk_handles_null() {
        try (TestContainer tc = new TestContainer()) {
            assertNull(CBJSON.walk((JSONObject) null));
        }
    }

    @Test
    public void walk_returns_outer_if_no_keys() {
        try (TestContainer tc = new TestContainer()) {
            JSONObject object = new JSONObject();
            assertThat(CBJSON.walk(object), is(sameInstance(object)));
        }
    }

    @Test
    public void walk_returns_inner_JSONObject() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject y = new JSONObject();
            JSONObject x = new JSONObject();
            x.put("y", y);
            JSONObject object = new JSONObject();
            object.put("x", x);
            assertThat(CBJSON.walk(object, "x"), is(sameInstance(x)));
            assertThat(CBJSON.walk(object, "x", "y"), is(sameInstance(y)));
        }
    }

    @Test
    public void walk_returns_null_if_not_found() throws JSONException {
        try (TestContainer tc = new TestContainer()) {
            JSONObject y = new JSONObject();
            JSONObject x = new JSONObject();
            x.put("y", y);
            JSONObject object = new JSONObject();
            object.put("x", x);
            assertNull(CBJSON.walk(object, "x", "a"));
            assertNull(CBJSON.walk(object, "x", "y", "b"));
        }
    }
}
