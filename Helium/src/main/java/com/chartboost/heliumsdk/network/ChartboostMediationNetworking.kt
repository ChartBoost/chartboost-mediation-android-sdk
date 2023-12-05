/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import androidx.annotation.VisibleForTesting
import com.chartboost.heliumsdk.controllers.PartnerController
import com.chartboost.heliumsdk.controllers.PrivacyController
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.network.Endpoints.BASE_DOMAIN
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event
import com.chartboost.heliumsdk.network.model.*
import com.chartboost.heliumsdk.network.model.ChartboostMediationHeaderMap.*
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.HeliumJson
import com.chartboost.heliumsdk.utils.LogController
import com.chartboost.heliumsdk.utils.MacroHelper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.UnknownHostException

/**
 * @suppress
 */
internal object ChartboostMediationNetworking {

    const val APP_SET_ID_HEADER_KEY = "x-mediation-idfv"
    const val AUCTION_ID_HEADERY_KEY = "x-mediation-auction-id"
    const val INIT_HASH_HEADER_KEY = "x-helium-sdk-init-hash"
    const val RATE_LIMIT_HEADER_KEY = "X-Helium-Ratelimit-Reset"
    const val SESSION_ID_HEADER_KEY = "X-Helium-SessionID"
    const val SDK_VERSION_HEADER_KEY = "X-Helium-SDK-Version"
    const val DEVICE_OS_HEADER_KEY = "X-Helium-Device-OS"
    const val DEVICE_OS_VERSION_HEADER_KEY = "X-Helium-Device-OS-Version"
    const val MEDIATION_LOAD_ID_HEADER_KEY = "X-Mediation-Load-ID"

    private const val REWARDED_CALLBACK_DELAY_MS = 1000L

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConverter = HeliumJson.asConverterFactory(
        "application/json; charset=utf-8".toMediaType()
    )

    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

    // by lazy is necessary for changing the url for testing
    @VisibleForTesting
    val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_DOMAIN)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(jsonConverter)
            .build()
    }

    // by lazy is necessary for changing the url for testing
    @VisibleForTesting
    val api by lazy {
        retrofitInstance.create(ChartboostMediationApi::class.java)
    }

    @Retention(AnnotationRetention.SOURCE)
    annotation class Method {
        companion object {
            var GET = "GET"
            var POST = "POST"
        }
    }

    suspend fun trackChartboostImpression(
        auctionID: String?,
        loadId: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            ImpressionRequestBody(auctionID).let {
                api.trackChartboostImpression(
                    url = Event.HELIUM_IMPRESSION.endpoint,
                    headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                    body = it
                )
            }
        }
    }

    suspend fun trackPartnerImpression(
        sessionId: String,
        appSetId: String,
        auctionID: String?,
        loadId: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        return safeApiCall {
            ImpressionRequestBody(auctionID).let {
                api.trackPartnerImpression(
                    url = Event.PARTNER_IMPRESSION.endpoint,
                    sessionId = sessionId,
                    appSetId = appSetId,
                    loadId = loadId,
                    body = it
                )
            }
        }
    }

    suspend fun trackClick(
        auctionId: String,
        loadId: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            SimpleTrackingRequestBody(auctionId).let {
                api.trackClick(
                    url = Event.CLICK.endpoint,
                    headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                    body = it
                )
            }
        }
    }

    suspend fun trackReward(
        auctionId: String,
        loadId: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            SimpleTrackingRequestBody(auctionId).let {
                api.trackReward(
                    url = Event.REWARD.endpoint,
                    headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                    body = it
                )
            }
        }
    }

    suspend fun trackAdLoad(
        placementName: String,
        adType: String,
        loadId: String,
        status: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            AdLoadNotificationRequestBody(placementName, adType, loadId, status).let {
                api.trackAdLoad(
                    url = Event.ADLOAD.endpoint,
                    headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                    body = it
                )
            }
        }
    }

    suspend fun trackEvent(
        event: Event,
        loadId: String?,
        metricsRequestBody: MetricsRequestBody,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            api.trackEvent(
                url = event.endpoint,
                headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                body = metricsRequestBody
            )
        }
    }

    suspend fun trackAdaptiveBannerSize(
        loadId: String?,
        bannerSizeBody: BannerSizeBody
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            api.trackAdaptiveBannerSize(
                url = Event.BANNER_SIZE.endpoint,
                headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                body = bannerSizeBody
            )
        }
    }

    suspend fun logAuctionWinner(
        bids: Bids,
        loadId: String
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            AuctionWinnerRequestBody(bids).let {
                api.logAuctionWinner(
                    url = Event.WINNER.endpoint,
                    headers = ChartboostMediationAdLifecycleHeaderMap(loadId, appSetId),
                    body = it
                )
            }
        }
    }

    suspend fun makeRewardedCallbackRequest(
        activeBid: Bid,
        customData: String,
        rewardedCallbackData: RewardedCallbackData
    ): ChartboostMediationNetworkingResult<Unit?> {
        val macroHelper = activeBid.let {
            MacroHelper(
                System.currentTimeMillis(),
                customData,
                it.adRevenue,
                it.cpmPrice,
                it.partnerName
            )
        }

        val url = macroHelper.replaceMacros(rewardedCallbackData.url, true)

        var result: ChartboostMediationNetworkingResult<Unit?> =
            ChartboostMediationNetworkingResult.Failure(
                code = -1,
                headers = null,
                error = ChartboostMediationError.CM_INTERNAL_ERROR
            )

        var remainingRetries = rewardedCallbackData.maxRetries

        while (remainingRetries > 0 && result is ChartboostMediationNetworkingResult.Failure) {
            remainingRetries--
            result = safeApiCall {
                if (rewardedCallbackData.method == Method.POST) {
                    api.makeRewardedCallbackPostRequest(
                        url,
                        HeliumJson.parseToJsonElement(
                            macroHelper.replaceMacros(rewardedCallbackData.body, false)
                        )
                    )
                } else {
                    api.makeRewardedCallbackGetRequest(url)
                }
            }
            // delay before retrying. only retry (and therefore delay) on failure
            if (result is ChartboostMediationNetworkingResult.Failure) {
                delay(REWARDED_CALLBACK_DELAY_MS)
            }
        }

        return result
    }

    suspend fun makeBidRequest(
        privacyController: PrivacyController,
        partnerController: PartnerController,
        adLoadParams: AdLoadParams,
        bidTokens: Map<String, Map<String, String>>,
        rateLimitHeaderValue: String,
        impressionDepth: Int
    ): ChartboostMediationNetworkingResult<BidsResponse?> {
        val bidRequestBody = BidRequestBody(
            adLoadParams = adLoadParams,
            partnerController = partnerController,
            privacyController = privacyController,
            impressionDepth = impressionDepth,
            bidTokens = bidTokens,
        )

        val appSetId = Environment.fetchAppSetId()

        return safeApiCall {
            api.makeBidRequest(
                url = Endpoints.Rtb.AUCTIONS.endpoint,
                headers = ChartboostBidRequestMediationHeaderMap(
                    rateLimitHeaderValue,
                    adLoadParams.loadId,
                    appSetId
                ),
                body = bidRequestBody
            )
        }
    }

    suspend fun getAppConfig(
        appId: String,
        initHash: String,
        appSetId: String,
    ): ChartboostMediationNetworkingResult<AppConfig?> {
        return safeApiCall<AppConfig> {
            api.getConfig(
                url = "${Endpoints.Sdk.SDK_INIT.endpoint}/$appId",
                headers = ChartboostMediationAppConfigHeaderMap(initHash, appSetId)
            )
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T> safeApiCall(crossinline apiCall: suspend () -> Response<String>): ChartboostMediationNetworkingResult<T?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall.invoke()

                ChartboostMediationNetworkingResult.makeResult(
                    response = response,
                    error = NetworkErrorTransformer.transform(response)
                )
            } catch (throwable: Throwable) {
                LogController.e("Error making network request: ${throwable.message}")
                ChartboostMediationNetworkingResult.Failure(
                    code = -1,
                    headers = null,
                    error = if (throwable is UnknownHostException) {
                        ChartboostMediationError.CM_NO_CONNECTIVITY
                    } else {
                        ChartboostMediationError.CM_UNKNOWN_ERROR
                    },
                    throwable = throwable
                )
            }
        }
    }
}
