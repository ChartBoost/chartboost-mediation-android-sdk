/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import androidx.annotation.VisibleForTesting
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.controllers.PartnerController
import com.chartboost.chartboostmediationsdk.controllers.PrivacyController
import com.chartboost.chartboostmediationsdk.domain.AdFormat
import com.chartboost.chartboostmediationsdk.domain.AdLoadParams
import com.chartboost.chartboostmediationsdk.domain.AppConfig
import com.chartboost.chartboostmediationsdk.domain.Bid
import com.chartboost.chartboostmediationsdk.domain.Bids
import com.chartboost.chartboostmediationsdk.domain.BidsResponse
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.domain.RewardedCallbackData
import com.chartboost.chartboostmediationsdk.network.Endpoints.BASE_DOMAIN
import com.chartboost.chartboostmediationsdk.network.model.AuctionWinnerRequestBody
import com.chartboost.chartboostmediationsdk.network.model.BannerSizeBody
import com.chartboost.chartboostmediationsdk.network.model.BidRequestBody
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap.ChartboostBidRequestMediationHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap.ChartboostMediationAdLifecycleHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap.ChartboostMediationAppConfigHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationHeaderMap.ChartboostQueueRequestMediationHeaderMap
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.model.ImpressionRequestBody
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.network.model.PartnerImpressionRequestBody
import com.chartboost.chartboostmediationsdk.network.model.QueueRequestBody
import com.chartboost.chartboostmediationsdk.network.model.SimpleTrackingRequestBody
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.chartboostmediationsdk.utils.MacroHelper
import com.chartboost.core.ChartboostCore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
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
    const val AUCTION_ID_HEADER_KEY = "x-mediation-auction-id"
    const val INIT_HASH_HEADER_KEY = "x-mediation-sdk-init-hash"
    const val RATE_LIMIT_HEADER_KEY = "x-mediation-ratelimit-reset"
    const val OLD_RATE_LIMIT_HEADER_KEY = "x-helium-ratelimit-reset"
    const val SESSION_ID_HEADER_KEY = "x-mediation-session-id"
    const val SDK_VERSION_HEADER_KEY = "x-mediation-sdk-version"
    const val DEVICE_OS_HEADER_KEY = "x-mediation-device-os"
    const val DEVICE_OS_VERSION_HEADER_KEY = "x-mediation-device-os-version"
    const val MEDIATION_LOAD_ID_HEADER_KEY = "x-mediation-load-id"
    const val DEBUG_HEADER_KEY = "x-mediation-debug"
    const val QUEUE_ID_HEADER_KEY = "x-mediation-queue-id"
    const val APP_ID_HEADER_KEY = "x-mediation-app-id"
    const val AD_TYPE_HEADER_KEY = "x-mediation-ad-type"
    const val MEDIATION_VERSION_GIT_HASH_HEADER_KEY = "x-mediation-sdk-version-short-git-hash"

    /**
     * Custom interceptor that is settable via reflection for debugging purposes
     */
    private var customInterceptor: Interceptor? = null

    private const val REWARDED_CALLBACK_DELAY_MS = 1000L

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConverter =
        ChartboostMediationJson.asConverterFactory(
            "application/json; charset=utf-8".toMediaType(),
        )

    private val client: OkHttpClient by lazy {
        // Apply the logging interceptor if the log level is appropriate
        val builder = OkHttpClient.Builder().addInterceptor(createHttpLoggingInterceptor())

        // Apply custom interceptor if it's provided
        customInterceptor?.let { builder.addInterceptor(it) }
        builder.build()
    }

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // by lazy is necessary for changing the url for testing
    @VisibleForTesting
    val retrofitInstance: Retrofit by lazy {
        Retrofit
            .Builder()
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
        bids: Bids,
        loadId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            ImpressionRequestBody(bids).let {
                api.trackChartboostImpression(
                    url = Endpoints.Event.HELIUM_IMPRESSION.endpoint,
                    headers =
                        ChartboostMediationAdLifecycleHeaderMap(
                            loadId = loadId,
                            appSetId = appSetId,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }
    }

    suspend fun trackPartnerImpression(
        appSetId: String,
        auctionID: String?,
        loadId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> =
        safeApiCall {
            PartnerImpressionRequestBody(auctionID).let {
                api.trackPartnerImpression(
                    url = Endpoints.Event.PARTNER_IMPRESSION.endpoint,
                    headers =
                        ChartboostMediationAdLifecycleHeaderMap(
                            loadId = loadId,
                            appSetId = appSetId,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }

    suspend fun trackClick(
        auctionId: String,
        loadId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            SimpleTrackingRequestBody(auctionId).let {
                api.trackClick(
                    url = Endpoints.Event.CLICK.endpoint,
                    headers =
                        ChartboostMediationAdLifecycleHeaderMap(
                            loadId = loadId,
                            appSetId = appSetId,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }
    }

    suspend fun trackReward(
        auctionId: String,
        loadId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            SimpleTrackingRequestBody(auctionId).let {
                api.trackReward(
                    url = Endpoints.Event.REWARD.endpoint,
                    headers =
                        ChartboostMediationAdLifecycleHeaderMap(
                            loadId = loadId,
                            appSetId = appSetId,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }
    }

    suspend fun trackEvent(
        event: Endpoints.Event,
        loadId: String?,
        queueId: String?,
        metricsRequestBody: MetricsRequestBody,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            api.trackEvent(
                url = event.endpoint,
                headers =
                    ChartboostMediationAdLifecycleHeaderMap(
                        loadId = loadId,
                        appSetId = appSetId,
                        queueId = queueId,
                        adType = metricsRequestBody.placementType ?: "",
                        appId = ChartboostMediationSdk.getAppId() ?: "",
                    ),
                body = metricsRequestBody,
            )
        }
    }

    suspend fun trackAdaptiveBannerSize(
        loadId: String?,
        bannerSizeBody: BannerSizeBody,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            api.trackAdaptiveBannerSize(
                url = Endpoints.Event.BANNER_SIZE.endpoint,
                headers =
                    ChartboostMediationAdLifecycleHeaderMap(
                        loadId = loadId,
                        appSetId = appSetId,
                        queueId = null,
                        adType = AdFormat.ADAPTIVE_BANNER.name,
                        appId = ChartboostMediationSdk.getAppId() ?: "",
                    ),
                body = bannerSizeBody,
            )
        }
    }

    suspend fun logAuctionWinner(
        bids: Bids,
        loadId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            AuctionWinnerRequestBody(bids).let {
                api.logAuctionWinner(
                    url = Endpoints.Event.WINNER.endpoint,
                    headers =
                        ChartboostMediationAdLifecycleHeaderMap(
                            loadId = loadId,
                            appSetId = appSetId,
                            queueId = null,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }
    }

    suspend fun makeRewardedCallbackRequest(
        activeBid: Bid,
        customData: String,
        rewardedCallbackData: RewardedCallbackData,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val macroHelper =
            activeBid.let {
                MacroHelper(
                    System.currentTimeMillis(),
                    customData,
                    it.adRevenue,
                    it.cpmPrice,
                    it.partnerName,
                )
            }

        val url = macroHelper.replaceMacros(rewardedCallbackData.url, true)

        var result: ChartboostMediationNetworkingResult<Unit?> =
            ChartboostMediationNetworkingResult.Failure(
                code = -1,
                headers = null,
                error = ChartboostMediationError.OtherError.InternalError,
            )

        var remainingRetries = rewardedCallbackData.maxRetries

        while (remainingRetries > 0 && result is ChartboostMediationNetworkingResult.Failure) {
            remainingRetries--
            result =
                safeApiCall {
                    if (rewardedCallbackData.method == Method.POST) {
                        api.makeRewardedCallbackPostRequest(
                            url,
                            ChartboostMediationJson.parseToJsonElement(
                                macroHelper.replaceMacros(rewardedCallbackData.body, false),
                            ),
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
        impressionDepth: Int,
    ): ChartboostMediationNetworkingResult<BidsResponse?> {
        val bidRequestBody =
            BidRequestBody(
                adLoadParams = adLoadParams,
                partnerController = partnerController,
                privacyController = privacyController,
                impressionDepth = impressionDepth,
                bidTokens = bidTokens,
            )

        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            api.makeBidRequest(
                url = Endpoints.Auction.AUCTION_NONTRACKING.endpoint,
                headers =
                    ChartboostBidRequestMediationHeaderMap(
                        rateLimitHeaderValue,
                        adLoadParams.loadId,
                        appSetId,
                        adLoadParams.adIdentifier.placementType,
                        ChartboostMediationSdk.getAppId() ?: "",
                    ),
                body = bidRequestBody,
            )
        }
    }

    suspend fun makeQueueRequest(
        isRunning: Boolean,
        placement: String,
        queueCapacity: Int,
        actualMaxQueueSize: Int? = null,
        queueDepth: Int,
        queueId: String,
        adType: String,
    ): ChartboostMediationNetworkingResult<Unit?> {
        val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""

        return safeApiCall {
            QueueRequestBody(
                placement = placement,
                queueCapacity = queueCapacity,
                actualMaxQueueSize = actualMaxQueueSize,
                currentQueueDepth = queueDepth,
                queueId = queueId,
            ).let {
                api.trackQueueRequest(
                    url =
                        when (isRunning) {
                            true -> Endpoints.Event.START_QUEUE.endpoint
                            false -> Endpoints.Event.END_QUEUE.endpoint
                        },
                    headers =
                        ChartboostQueueRequestMediationHeaderMap(
                            queueId = queueId,
                            appSetId = appSetId,
                            adType = adType,
                            appId = ChartboostMediationSdk.getAppId() ?: "",
                        ),
                    body = it,
                )
            }
        }
    }

    suspend fun getAppConfig(
        appId: String,
        initHash: String,
        appSetId: String,
    ): ChartboostMediationNetworkingResult<AppConfig?> =
        safeApiCall<AppConfig> {
            api.getConfig(
                url = "${Endpoints.Sdk.SDK_INIT.endpoint}/$appId",
                headers =
                    ChartboostMediationAppConfigHeaderMap(
                        initHash = initHash,
                        appSetId = appSetId,
                        appId = appId,
                    ),
            )
        }

    @PublishedApi
    internal suspend inline fun <reified T> safeApiCall(
        crossinline apiCall: suspend () -> Response<String>,
    ): ChartboostMediationNetworkingResult<T?> =
        withContext(ioDispatcher) {
            try {
                val response = apiCall.invoke()

                ChartboostMediationNetworkingResult.makeResult(
                    response = response,
                    error = NetworkErrorTransformer.transform(response),
                )
            } catch (throwable: Throwable) {
                LogController.e("Error making network request: ${throwable.message}")
                ChartboostMediationNetworkingResult.Failure(
                    code = -1,
                    headers = null,
                    error =
                        if (throwable is UnknownHostException) {
                            ChartboostMediationError.OtherError.NoConnectivity
                        } else {
                            ChartboostMediationError.LoadError.NetworkingError
                        },
                    throwable = throwable,
                )
            }
        }

    private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logLevel =
            when (LogController.logLevel) {
                LogController.LogLevel.VERBOSE, LogController.LogLevel.DEBUG -> HttpLoggingInterceptor.Level.BODY
                LogController.LogLevel.INFO -> HttpLoggingInterceptor.Level.BASIC
                LogController.LogLevel.WARNING -> HttpLoggingInterceptor.Level.HEADERS
                LogController.LogLevel.ERROR, LogController.LogLevel.DISABLED -> HttpLoggingInterceptor.Level.NONE
            }

        return HttpLoggingInterceptor { message ->
            // Only log if the configured log level is INFO or more verbose
            if (LogController.logLevel.value >= LogController.LogLevel.INFO.value) {
                Platform.get().log(message)
            }
        }.apply {
            level = logLevel
        }
    }
}
