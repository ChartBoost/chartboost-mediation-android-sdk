/*
 * Copyright 2024-2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

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

    internal const val DEFAULT_INITIALIZATION_EVENT_URL = "${SCHEME}initialization.mediation-sdk.chartboost.com/v1/event/initialization"

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
}
