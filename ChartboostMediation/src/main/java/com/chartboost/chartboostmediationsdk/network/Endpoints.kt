/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

/**
 * @suppress
 *
 * A class for the Chartboost Mediation SDK endpoints.
 */
object Endpoints {
    /**
     * URL endpoints.
     */

    private const val SCHEME = "https://"

    var SDK_HOSTNAME = "mediation-sdk.chartboost.com"
        internal set

    internal const val BASE_DOMAIN = "${SCHEME}chartboost.com"

    /**
     * Various endpoints have a version associated with them.
     * Any new versions introduced can be added here.
     */
    enum class Version {
        V0,
        V1,
        V2,
        V3,
        V4,
        ;

        /**
         * Lowercase the name of the Version enums (ie: __V1__ ~> __v1__).
         */
        override fun toString(): String = this.name.lowercase()
    }

    /**
     * Sdk endpoints associated with the __[SDK_DOMAIN]__ that are not associated with events and don't
     * require an event path.
     */
    enum class Sdk(
        private val hostname: String,
        val version: Version,
    ) {
        SDK_INIT("config", Version.V1),
        ;

        /**
         * Creates a String URL for the particular [Sdk] enum.
         * An event URL will generally look as follows:
         * __https://[Sdk.hostname].mediation-sdk.chartboost.com/[Version]/event/[Sdk.name]__
         */
        val endpoint
            get() = "${SCHEME}$hostname.$SDK_HOSTNAME/$version/${name.lowercase()}"
    }

    /**
     * Sdk endpoints associated with the __[SDK_DOMAIN]__ that are not associated with events and don't
     * require an event path.
     */
    enum class Auction(
        private val hostname: String,
        val version: Version,
    ) {
        AUCTION_NONTRACKING("non-tracking.auction", Version.V3),

        // Not currently used.
        AUCTION_TRACKING("tracking.auction", Version.V3),
        ;

        /**
         * Creates an auctions String URL for the particular [Sdk] enum with an auctions path.
         * An auctions URL will generally look as follows:
         * __https://[Sdk.hostname].mediation-sdk.chartboost.com/[Version]/auctions__
         */
        val endpoint
            get() = "${SCHEME}$hostname.$SDK_HOSTNAME/$version/auctions"
    }

    /**
     * Event endpoints associated with the __[SDK_DOMAIN]__ along with an event path.
     * Some of the events are currently only used for [MetricsRequest] while others
     * are used during an ad's cycle.
     */
    @Serializable
    enum class Event(
        private val hostname: String,
        val version: Version,
    ) {
        BANNER_SIZE("banner-size", Version.V1),
        CLICK("click", Version.V2),
        CONFIG("config", Version.V1),
        END_QUEUE("end-queue", Version.V1),
        EXPIRATION("expiration", Version.V1),
        HELIUM_IMPRESSION("mediation-impression", Version.V2),
        INITIALIZATION("initialization", Version.V1),
        LOAD("load", Version.V2),
        PARTNER_IMPRESSION("partner-impression", Version.V1),
        PREBID("prebid", Version.V1),
        REWARD("reward", Version.V2),
        SHOW("show", Version.V1),
        START_QUEUE("start-queue", Version.V1),
        WINNER("winner", Version.V3),
        ;

        /**
         * Creates a String URL for the particular [Event] enum.
         * An event URL will generally look as follows:
         * __https://[Event.hostname].mediation-sdk.chartboost.com/[Version]/event/[Event.name]__
         */

        val endpoint
            get() = "${SCHEME}$hostname.$SDK_HOSTNAME/$version/event/${name.lowercase()}"

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
                return jsonArray.mapNotNull { jsonElement ->
                    runCatching { Event.valueOf(jsonElement.jsonPrimitive.content) }.getOrNull()
                }.toCollection(EnumSet.noneOf(Event::class.java))
            }
        }
    }
}
