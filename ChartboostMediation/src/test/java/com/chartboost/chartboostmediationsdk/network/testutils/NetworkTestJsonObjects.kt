/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.testutils

import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

enum class NetworkTestJsonObjects(
    private val path: String,
) {
    // Simple request and success
    SIMPLE_AUCTION_ID_REQUEST("test.network/simple_auction_id_request.json"),
    HTTP_200_SIMPLE_SUCCESS("test.network/http_200_simple_success.json"),

    // SDK_INIT
    HTTP_200_SDK_INIT_SUCCESS("test.network/http_200_sdk_init_success.json"),
    HTTP_200_SDK_INIT_FAILURE("test.network/http_200_sdk_init_failure.json"),

    // AUCTION
    BAD_BID_REQUEST("test.auctions/bad_bid_request.json"),
    BAD_BANNER_BID_RESPONSE("test.auctions/bad_banner_bid_response.json"),
    BAD_INTERSTITIAL_BID_RESPONSE("test.auctions/bad_interstitial_bid_response.json"),
    BAD_REWARDED_BID_RESPONSE("test.auctions/bad_rewarded_bid_response.json"),

    BANNER_BID_REQUEST("test.auctions/banner_bid_request.json"),
    INTERSTITIAL_BID_REQUEST("test.auctions/interstitial_bid_request.json"),
    REWARDED_BID_REQUEST("test.auctions/rewarded_bid_request.json"),

    LOG_AUCTION_WINNER_REQUEST("test.auctions/log_auction_winner_request.json"),

    BANNER_BID_RESPONSE("test.auctions/banner_bid_response.json"),
    INTERSTITIAL_BID_RESPONSE("test.auctions/interstitial_bid_response.json"),
    REWARDED_BID_RESPONSE_GET_CALLBACK("test.auctions/rewarded_bid_response_get_callback.json"),
    REWARDED_BID_RESPONSE_POST_CALLBACK("test.auctions/rewarded_bid_response_post_callback.json"),

    TRACK_EVENT_INITIALIZATION_SUCCESS_REQUEST("test.events/track_event_initialization_success_request.json"),
    TRACK_EVENT_INITIALIZATION_SUCCESS_WITH_ERROR_REQUEST("test.events/track_event_initialization_success_with_error_request.json"),
    TRACK_EVENT_INITIALIZATION_FAILURE_REQUEST("test.events/track_event_initialization_failure_request.json"),

    TRACK_AD_LOAD_REQUEST("test.events/track_ad_load.json"),
    ;

    val trimmedJsonString
        get() = rawJsonString.trim()

    val rawJsonString
        get() = MockResponseFileReader(path).content.trim()

    val minifiedJsonString
        get() =
            ChartboostMediationJson.encodeToString(
                ChartboostMediationJson.decodeFromString(JsonElement.serializer(), rawJsonString),
            )
}
