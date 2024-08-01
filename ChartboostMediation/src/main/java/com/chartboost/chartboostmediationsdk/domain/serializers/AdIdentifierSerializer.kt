/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain.serializers

import com.chartboost.chartboostmediationsdk.domain.AdIdentifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @suppress
 */
object AdIdentifierSerializer : KSerializer<AdIdentifier> {
    override fun deserialize(decoder: Decoder): AdIdentifier {
        val adType = decoder.decodeInt()
        val placement = decoder.decodeString()
        return AdIdentifier(adType, placement)
    }

    override fun serialize(
        encoder: Encoder,
        value: AdIdentifier,
    ) {
        encoder.encodeInt(value.adType)
        encoder.encodeString(value.placement)
    }

    override val descriptor: SerialDescriptor
        get() =
            buildClassSerialDescriptor(AdIdentifier::class.qualifiedName!!) {
                element<Int>("placement")
                element<String>("adType")
            }
}
