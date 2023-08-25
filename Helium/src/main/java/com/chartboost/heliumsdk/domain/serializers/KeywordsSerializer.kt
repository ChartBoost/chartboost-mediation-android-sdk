/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain.serializers

import com.chartboost.heliumsdk.domain.Keywords
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @suppress
 */
object KeywordsSerializer : KSerializer<Keywords> {
    override fun deserialize(decoder: Decoder): Keywords {
        val keywords = decoder.decodeSerializableValue(
            MapSerializer(String.serializer(), String.serializer())
        )
        return Keywords().apply {
            keywords.entries.forEach { entry ->
                set(entry.key, entry.value)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Keywords) {
        delegate.serialize(
            encoder,
            value.get().takeIf { it.isNotEmpty() } ?: emptyMap()
        )
    }

    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
}
