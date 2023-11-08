/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.util.Base64
import com.chartboost.heliumsdk.domain.MetricsError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule

/**
 * By using HeliumJson instead of Json where ever serialization and deserialization is happening,
 * we're able to guarantee that we're always using the same configuration
 */
@OptIn(ExperimentalSerializationApi::class)
@PublishedApi internal val HeliumJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    serializersModule = SerializersModule {
        polymorphic(
            MetricsError::class,
            MetricsError.SimpleError::class,
            MetricsError.SimpleError.serializer()
        )
        polymorphic(
            MetricsError::class,
            MetricsError.JsonParseError::class,
            MetricsError.JsonParseError.serializer()
        )
    }
}

/**
 * @suppress
 */
fun getMaxJsonPayload(jsonString: String, maxSize: Int): String {
    val jsonBase64Encoded = Base64.encode(jsonString.toByteArray(), Base64.NO_WRAP)

    return if (jsonBase64Encoded.size > maxSize) {
        Base64.encode(
            "The malformed JSON is too large to include. Partial JSON: $jsonString".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        ).copyOfRange(0, maxSize - maxSize % 4).toString(Charsets.UTF_8)
    } else {
        Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
    }
}
