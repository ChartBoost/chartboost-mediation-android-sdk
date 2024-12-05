/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

/**
 * @suppress
 */
@Throws(SerializationException::class, IllegalArgumentException::class)
fun JSONObject.toJsonObject(): JsonObject {
    val jsonString = this.toString()
    // We shouldn't need to handle the possible exception because we're already (in theory) dealing
    // with valid json
    return HeliumJson.decodeFromString(jsonString)
}

/**
 * @suppress
 */
@Throws(SerializationException::class, IllegalArgumentException::class)
fun JsonObject.toJSONObject(): JSONObject {
    val jsonString = Json.encodeToString(this)
    // We shouldn't need to handle the possible exception because we're already (in theory) dealing
    // with valid json
    return JSONObject(jsonString)
}
