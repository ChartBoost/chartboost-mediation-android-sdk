package com.chartboost.sdk.test;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.CBJSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class JSONObjectBuilder {
    private final JSONObject built;

    protected JSONObjectBuilder() {
        this.built = new JSONObject();
    }

    protected JSONObjectBuilder(JSONObject built) {
        this.built = built;
    }

    @NonNull
    public JSONObject build() {
        return copy();
    }

    @NonNull
    public JSONObject toJSONObject() {
        return copy();
    }

    @NonNull
    protected JSONObject copy() {
        try {
            return new JSONObject(built.toString());
        } catch (JSONException e) {
            throw new Error(e);
        }
    }

    @NonNull
    protected static JSONObject subObject(final JSONObject parent, String... path) {
        JSONObject result = parent;
        for (String name : path) {
            JSONObject sub = result.optJSONObject(name);
            if (sub == null) {
                sub = new JSONObject();
                CBJSON.put(result, name, sub);
            }
            result = sub;
        }
        return result;
    }

    @NonNull
    protected static JSONArray subArray(final JSONObject parent, String... path) {
        String[] arrayParentPath = Arrays.copyOf(path, path.length - 1);
        String arrayName = path[path.length - 1];
        JSONObject arrayParent = subObject(parent, arrayParentPath);
        JSONArray array = arrayParent.optJSONArray(arrayName);
        if (array == null) {
            array = new JSONArray();
            CBJSON.put(arrayParent, arrayName, array);
        }
        return array;
    }
}
