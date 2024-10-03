/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.network.model.AuctionWinnerRequestBody
import com.chartboost.chartboostmediationsdk.network.model.BannerSizeBody
import com.chartboost.chartboostmediationsdk.network.model.BidRequestBody
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap.ChartboostMediationAppConfigHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ImpressionRequestBody
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.network.model.PartnerImpressionRequestBody
import com.chartboost.chartboostmediationsdk.network.model.QueueRequestBody
import com.chartboost.chartboostmediationsdk.network.model.SimpleTrackingRequestBody
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * @suppress
 */
interface ChartboostMediationApi {
    @POST
    suspend fun logAuctionWinner(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: AuctionWinnerRequestBody,
    ): Response<String>

    @POST
    suspend fun trackChartboostImpression(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: ImpressionRequestBody,
    ): Response<String>

    @POST
    suspend fun trackPartnerImpression(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: PartnerImpressionRequestBody,
    ): Response<String>

    @POST
    suspend fun trackClick(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: SimpleTrackingRequestBody,
    ): Response<String>

    @POST
    suspend fun trackReward(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: SimpleTrackingRequestBody,
    ): Response<String>

    @POST
    suspend fun trackEvent(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: MetricsRequestBody,
    ): Response<String>

    @POST
    suspend fun trackAdaptiveBannerSize(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap,
        @Body body: BannerSizeBody,
    ): Response<String>

    @POST
    suspend fun makeRewardedCallbackPostRequest(
        @Url callbackUrl: String,
        @Body body: JsonElement,
    ): Response<String>

    @POST
    suspend fun makeBidRequest(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostBidRequestMediationHeaderMap,
        @Body body: BidRequestBody,
    ): Response<String>

    @POST
    suspend fun trackQueueRequest(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationHeaderMap.ChartboostQueueRequestMediationHeaderMap,
        @Body body: QueueRequestBody,
    ): Response<String>

    @GET
    suspend fun makeRewardedCallbackGetRequest(
        @Url callbackUrl: String,
    ): Response<String>

    @GET
    suspend fun getConfig(
        @Url url: String,
        @HeaderMap headers: ChartboostMediationAppConfigHeaderMap,
    ): Response<String>
}
