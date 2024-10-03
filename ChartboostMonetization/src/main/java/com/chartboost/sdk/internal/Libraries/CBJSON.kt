package com.chartboost.sdk.internal.Libraries

import com.chartboost.sdk.internal.logging.Logger.e
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Put into a JSONObject without throwing.
 * JSONObject.put throws if value is Double.NaN or Double.Inf
 */
fun JSONObject.safePut(
    name: String,
    value: Any?,
): JSONObject {
    try {
        this.put(name, value)
    } catch (ex: JSONException) {
        e("put ($name)$ex")
    }
    return this
}

/**
 * Returns the value mapped by name if it exists and is a boolean or can be coerced to a boolean, or null otherwise.
 */
fun JSONObject.getBooleanOrNull(name: String): Boolean? {
    return try {
        getBoolean(name)
    } catch (ignored: JSONException) {
        null
    }
}

/**
 * Transform JsonArray to ByteArray
 */
fun JSONArray.toByteArray(): ByteArray = toString().toByteArray()
