/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.Serializable

/**
 * Sdk Event enum
 * Some of the events are currently only used for tracking events while others
 * are used to send additional metrics.
 */
@Serializable
enum class TrackingEvent(
    val serverName: String,
) {
    BANNER_SIZE("banner_size"),
    CLICK("click"),
    CONFIG("config"),
    END_QUEUE("end_queue"),
    EXPIRATION("expiration"),
    HELIUM_IMPRESSION("helium_impression"),
    INITIALIZATION("initialization"),
    LOAD("load"),
    PARTNER_IMPRESSION("partner_impression"),
    PREBID("prebid"),
    REWARD("reward"),
    SHOW("show"),
    START_QUEUE("start_queue"),
    WINNER("winner"),
}
