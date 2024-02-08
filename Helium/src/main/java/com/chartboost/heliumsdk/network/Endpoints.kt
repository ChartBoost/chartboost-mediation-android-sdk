/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

/**
 * @suppress
 *
 * A class for the Helium SDK endpoints.
 */
object Endpoints {
    /**
     * URL endpoints.
     */

    var SDK_DOMAIN = "https://helium-sdk.chartboost.com"
        internal set
    var RTB_DOMAIN = "https://helium-rtb.chartboost.com"
        internal set

    internal const val BASE_DOMAIN = "https://chartboost.com"

    /**
     * Various endpoints have a version associated with them.
     * Any new versions introduced can be added here.
     */
    enum class Version {
        V1,
        V2,
        V3,
        V4,
        ;

        /**
         * Lowercase the name of the Version enums (ie: __V1__ ~> __v1__).
         */
        override fun toString(): String {
            return this.name.lowercase()
        }
    }

    /**
     * Endpoints associated with the __helium-rtb.chartboost.com__ domain.
     */
    enum class Rtb(val version: Version) {
        AUCTIONS(Version.V3),
        ;

        /**
         * Endpoints associated with the __helium-rtb.chartboost.com__ domain with a config path.
         */
        enum class Config(val version: Version) {
            PLACEMENTS(Version.V4),
            ;

            /**
             * Creates a [String] URL for the particular [Config] enum.
             * A config URL will generally look as follows:
             * __helium-rtb.chartboost.com/[Version]/config/[Config.name]__
             */
            val endpoint
                get() = "$RTB_DOMAIN/$version/config/${name.lowercase()}"
        }

        /**
         * Creates a [String] URL for the particular [Rtb] enum.
         * An rtb URL will generally look as follows:
         * __helium-rtb.chartboost.com/[Version]/[Rtb.name]__
         */
        val endpoint
            get() = "$RTB_DOMAIN/$version/${name.lowercase()}"
    }

    /**
     * Endpoints associated with the __helium-sdk.chartboost.com__ domain.
     */
    enum class Sdk(val version: Version) {
        SDK_INIT(Version.V1),
        ;

        /**
         * Endpoints associated with the __helium-sdk.chartboost.com__ domain with an event path.
         * Some of the events are currently only used for [MetricsRequest] while others
         * are used during an ad's cycle.
         */
        @Serializable
        enum class Event(val version: Version) {
            BANNER_SIZE(Version.V1),
            ADLOAD(Version.V2),
            CLICK(Version.V2),
            EXPIRATION(Version.V1),
            HELIUM_IMPRESSION(Version.V1),
            INITIALIZATION(Version.V1),
            LOAD(Version.V2),
            PARTNER_IMPRESSION(Version.V1),
            PREBID(Version.V1),
            REWARD(Version.V2),
            SHOW(Version.V1),
            WINNER(Version.V3),
            ;

            /**
             * Creates a String URL for the particular [Event] enum.
             * An event URL will generally look as follows:
             * __helium-sdk.chartboost.com/[Version]/event/[Event.name]__
             */
            val endpoint
                get() = "$SDK_DOMAIN/$version/event/${name.lowercase()}"

            @Serializer(forClass = EnumSet::class)
            object EventEnumSetSerializer : KSerializer<EnumSet<Event>> {
                override val descriptor = JsonArray.serializer().descriptor

                override fun serialize(
                    encoder: Encoder,
                    value: EnumSet<Event>,
                ) {
                    val jsonArray = JsonArray(value.map { JsonPrimitive(it.name) })
                    encoder.encodeSerializableValue(JsonArray.serializer(), jsonArray)
                }

                override fun deserialize(decoder: Decoder): EnumSet<Event> {
                    val jsonArray = decoder.decodeSerializableValue(JsonArray.serializer())
                    return jsonArray.map { Event.valueOf(it.jsonPrimitive.content) }
                        .toCollection(EnumSet.noneOf(Event::class.java))
                }
            }
        }

        /**
         * Creates a String URL for the particular [Sdk] enum.
         * An Sdk URL will generally look as follows:
         * __helium-sdk.chartboost.com/[Version]/[Sdk.name]__
         */
        val endpoint
            get() = "$SDK_DOMAIN/$version/${name.lowercase()}"
    }
}
