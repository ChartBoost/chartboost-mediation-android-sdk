package com.chartboost.sdk.internal.utils

import org.json.JSONArray
import org.json.JSONObject

fun isJSONValid(test: String?): Boolean =
    runCatching { JSONObject(test) }.isSuccess ||
        runCatching { JSONArray(test) }.isSuccess
