package com.chartboost.sdk.internal.Libraries;

import com.chartboost.sdk.internal.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A series of helper methods related to JSON.
 */
public class CBJSON {
    // traverse a tree of JSONObjects.
    public static JSONObject walk(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object == null)
                break;
            object = object.optJSONObject(key);
        }
        return object;
    }

    /* Put into a JSONObject without throwing.
        JSONObject.put throws if value is Double.NaN or Double.Inf
     */
    public static void put(JSONObject obj, String name, Object value) {
        try {
            obj.put(name, value);
        } catch (JSONException ex) {
            Logger.e("put (" + name + ")", ex);
        }
    }

    // Like Dictionary(), but returns a JSONObject
    public static JSONObject jsonObject(JsonKV... kvs) {
        JSONObject json = new JSONObject();
        for (JsonKV kv : kvs) {
            put(json, kv.key, kv.value);
        }
        return json;

    }

    /**
     * Create a JSON key-value pair for use with {@link #jsonObject(JsonKV...)}
     */
    public static JsonKV JKV(String key, Object value) {
        return new JsonKV(key, value);
    }

    /**
     * Key value pair class for use with {@link #jsonObject(JsonKV...)}
     */
    public static class JsonKV {
        final String key;
        final Object value;

        public JsonKV(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

}
