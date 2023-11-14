/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking.SESSION_ID_HEADER_KEY
import com.chartboost.heliumsdk.network.model.*
import com.chartboost.heliumsdk.network.model.ChartboostMediationHeaderMap.ChartboostMediationAppConfigHeaderMap
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.*

/**
 * @suppress
 */
interface ChartboostMediationApi {

    @POST
    suspend fun logAuctionWinner(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: AuctionWinnerRequestBody
    ): Response<String>

    @POST
    suspend fun trackChartboostImpression(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: ImpressionRequestBody
    ): Response<String>

    @POST
    suspend fun trackPartnerImpression(
        @Url url: String,
        @Header(SESSION_ID_HEADER_KEY) sessionId: String,
        @Header(APP_SET_ID_HEADER_KEY) appSetId: String,
        @Header(MEDIATION_LOAD_ID_HEADER_KEY) loadId: String,
        @Body body: ImpressionRequestBody
    ): Response<String>

    @POST
    suspend fun trackClick(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: SimpleTrackingRequestBody
    ): Response<String>

    @POST
    suspend fun trackReward(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: SimpleTrackingRequestBody
    ): Response<String>

    @POST
    suspend fun trackAdLoad(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: AdLoadNotificationRequestBody
    ): Response<String>

    @POST
    suspend fun trackEvent(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: MetricsRequestBody
    ): Response<String>

    @POST
    suspend fun trackAdaptiveBannerSize(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: BannerSizeBody
    ): Response<String>

    @POST
    suspend fun makeRewardedCallbackPostRequest(
        @Url callbackUrl: String,
        @Body body: JsonElement
    ): Response<String>

    @POST
    suspend fun makeBidRequest(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap,
        @Body body: BidRequestBody
    ): Response<String>

    @GET
    suspend fun makeRewardedCallbackGetRequest(
        @Url callbackUrl: String,
    ): Response<String>

    @GET
    suspend fun getConfig(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationAppConfigHeaderMap
    ): Response<String>
}
