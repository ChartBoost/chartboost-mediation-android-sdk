/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray

/**
 * @suppress
 */
@Serializer(forClass = AdFormat::class)
object AdFormatEnumSetSerializer : KSerializer<AdFormat> {
    override val descriptor = JsonArray.serializer().descriptor

    override fun serialize(encoder: Encoder, value: AdFormat) {
        encoder.encodeSerializableValue(String.serializer(), value.name)
    }

    override fun deserialize(decoder: Decoder): AdFormat {
        return AdFormat.fromString(
            decoder.decodeSerializableValue(String.serializer())
        )
    }
}
