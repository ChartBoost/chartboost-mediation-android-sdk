/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.app.Activity
import android.content.Context
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.controllers.AdController
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.chartboostmediationsdk.utils.LogController.d
import com.chartboost.chartboostmediationsdk.utils.LogController.e
import com.chartboost.chartboostmediationsdk.utils.toJSONObject
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

/**
 * The Chartboost Mediation fullscreen ad. This class is responsible for showing and invalidating the ad.
 *
 * @property cachedAd The [CachedAd] to show.
 * @property listener The [ChartboostMediationFullscreenAdListener] to notify of ad events.
 * @property request The publisher-supplied [ChartboostMediationFullscreenAdLoadRequest] that was used to load the ad.
 * @property loadId The identifier for this load call.
 * @property winningBidInfo The winning bid info for the ad.
 * @property adController The ad controller to show the ad.
 */
class ChartboostMediationFullscreenAd(
    var cachedAd: CachedAd?,
    var listener: ChartboostMediationFullscreenAdListener?,
    var request: ChartboostMediationFullscreenAdLoadRequest,
    var loadId: String,
    var winningBidInfo: Map<String, String> = mapOf(),
    private val adController: AdController?,
) {
    /**
     * @suppress
     */
    companion object {
        /**
         * The maximum number of characters allowed for custom data.
         */
        private const val CUSTOM_DATA_MAX_CHAR = 1000

        /**
         * Load a fullscreen ad. This method is designed to be called from Java.
         *
         * @param context The [Context] to use for loading the ad.
         * @param request The publisher-supplied [ChartboostMediationFullscreenAdLoadRequest] containing relevant details to load the ad.
         * @param listener The [ChartboostMediationFullscreenAdListener] to notify of the ad lifecycle events.
         * @param adLoadListener The [ChartboostMediationFullscreenAdLoadListener] to notify of the ad load event.
         */
        @JvmStatic
        fun loadFullscreenAdFromJava(
            context: Context,
            request: ChartboostMediationFullscreenAdLoadRequest,
            listener: ChartboostMediationFullscreenAdListener,
            adLoadListener: ChartboostMediationFullscreenAdLoadListener,
        ) {
            CoroutineScope(Main).launch {
                val result = loadFullscreenAd(context, request, listener)
                adLoadListener.onAdLoaded(result)
            }
        }

        /**
         * Load a fullscreen ad.
         *
         * @param context The [Context] to use for loading the ad.
         * @param request The publisher-supplied [ChartboostMediationFullscreenAdLoadRequest] containing relevant details to load the ad.
         * @param listener The [ChartboostMediationFullscreenAdListener] to notify of ad events.
         *
         * @return The [ChartboostMediationFullscreenAdLoadResult] containing the result of the ad load.
         */
        suspend fun loadFullscreenAd(
            context: Context,
            request: ChartboostMediationFullscreenAdLoadRequest,
            listener: ChartboostMediationFullscreenAdListener,
        ): ChartboostMediationFullscreenAdLoadResult =
            loadFullscreenAd(context, request, ChartboostMediationSdk.chartboostMediationInternal.adController, listener)

        internal suspend fun loadFullscreenAd(
            context: Context,
            request: ChartboostMediationFullscreenAdLoadRequest,
            adController: AdController?,
            listener: ChartboostMediationFullscreenAdListener,
            queueId: String? = null,
        ): ChartboostMediationFullscreenAdLoadResult =
            withContext(Main) {
                val loadId = generateLoadId()
                val ad = createChartboostMediationFullscreenAd(loadId, request, adController, listener)
                val adFormat =
                    getAdFormat(request) ?: return@withContext createAdLoadResult(
                        null,
                        loadId,
                        createPayloadJson(mutableSetOf()),
                        ChartboostMediationError.LoadError.UnsupportedAdFormat,
                    )
                val adLoadParams = createAdLoadParams(ad, request, loadId, queueId, adFormat)
                val (metricsSet, loadResult) = performAdLoad(context, adLoadParams, adController)
                val payloadJson = createPayloadJson(metricsSet)
                val error = getError(loadResult)

                if (error != null) {
                    e("Failed to load fullscreen ad with error: $error")
                    return@withContext createAdLoadResult(null, loadId, payloadJson, error)
                }

                val cachedAd = getCachedAd(loadResult)

                ad.updateAdDetails(cachedAd, loadId, listener, request)
                createAdLoadResult(ad, loadId, payloadJson, null, cachedAd.winningBidInfo)
            }

        private fun generateLoadId(): String = "${ChartboostCore.analyticsEnvironment.appSessionIdentifier}${System.currentTimeMillis()}"

        private fun createChartboostMediationFullscreenAd(
            loadId: String,
            request: ChartboostMediationFullscreenAdLoadRequest,
            adController: AdController?,
            listener: ChartboostMediationFullscreenAdListener,
        ): ChartboostMediationFullscreenAd =
            ChartboostMediationFullscreenAd(
                cachedAd = null,
                listener = listener,
                request = request,
                loadId = loadId,
                adController = adController,
            )

        private fun getAdFormat(request: ChartboostMediationFullscreenAdLoadRequest): AdFormat? =
            AppConfigStorage.placementsToAdFormats?.get(request.placement)

        private fun createAdLoadParams(
            ad: ChartboostMediationFullscreenAd,
            request: ChartboostMediationFullscreenAdLoadRequest,
            loadId: String,
            queueId: String?,
            adFormat: AdFormat,
        ): AdLoadParams =
            AdLoadParams(
                adIdentifier = AdIdentifier(AdFormat.toAdType(adFormat), request.placement),
                keywords = request.keywords,
                loadId = loadId,
                queueId = queueId,
                bannerSize = null,
                adInteractionListener = createAdInteractionListener(ad),
                partnerSettings = request.partnerSettings,
            )

        private suspend fun performAdLoad(
            context: Context,
            adLoadParams: AdLoadParams,
            adController: AdController?,
        ): Pair<MutableSet<Metrics>, Result<CachedAd>?> =
            run {
                val metricsSet = mutableSetOf<Metrics>()
                var loadResult: Result<CachedAd>? = null

                CoroutineScope(Main)
                    .launch(
                        CoroutineExceptionHandler { _, error ->
                            loadResult = Result.failure(error)
                        },
                    ) {
                        loadResult = adController?.load(context, adLoadParams, metricsSet)
                            ?: Result.failure(
                                ChartboostMediationAdException(
                                    if (AppConfigStorage.shouldDisableSdk) {
                                        ChartboostMediationError.LoadError.Disabled
                                    } else {
                                        ChartboostMediationError.LoadError.ChartboostMediationNotInitialized
                                    },
                                ),
                            )
                    }.also { it.join() }

                return Pair(metricsSet, loadResult)
            }

        @OptIn(InternalSerializationApi::class)
        private fun createPayloadJson(metricsSet: MutableSet<Metrics>): JSONObject {
            val metricsRequestBody = MetricsManager.buildMetricsDataRequestBody(metricsSet)
            return ChartboostMediationJson
                .writeJson(
                    metricsRequestBody,
                    MetricsRequestBody.serializer(),
                ).jsonObject
                .toJSONObject()
        }

        private fun getError(loadResult: Result<CachedAd>?): ChartboostMediationError? =
            when (loadResult) {
                null -> ChartboostMediationError.LoadError.ChartboostMediationNotInitialized

                else ->
                    loadResult.fold({ null }, { throwable ->
                        if (throwable is ChartboostMediationAdException) {
                            throwable.chartboostMediationError
                        } else {
                            ChartboostMediationError.LoadError.Exception
                        }
                    })
            }

        private fun getCachedAd(loadResult: Result<CachedAd>?): CachedAd =
            loadResult?.getOrNull() ?: throw ChartboostMediationAdException(
                ChartboostMediationError.LoadError.ChartboostMediationNotInitialized,
            )

        private fun createAdLoadResult(
            ad: ChartboostMediationFullscreenAd?,
            loadId: String,
            payloadJson: JSONObject,
            error: ChartboostMediationError?,
            winningBidInfo: Map<String, String> = mapOf(),
        ) = ChartboostMediationFullscreenAdLoadResult(
            ad = ad,
            loadId = loadId,
            metrics = payloadJson,
            error = error,
            winningBidInfo,
        )

        private fun createAdInteractionListener(ad: ChartboostMediationFullscreenAd) =
            object : AdInteractionListener {
                override fun onImpressionTracked(partnerAd: PartnerAd) {
                    // We ignore the partner impression
                }

                override fun onClicked(partnerAd: PartnerAd) {
                    ad.listener?.onAdClicked(ad)
                        ?: e("Unable to notify onAdClicked() because listener is null")
                }

                override fun onRewarded(partnerAd: PartnerAd) {
                    ad.listener?.onAdRewarded(ad)
                        ?: e("Unable to notify onAdRewarded() because listener is null")
                }

                override fun onDismissed(
                    partnerAd: PartnerAd,
                    error: ChartboostMediationAdException?,
                ) {
                    ChartboostMediationSdk.chartboostMediationInternal.fullscreenAdShowingState.notifyFullscreenAdClosed()
                    ad.listener?.onAdClosed(ad, error)
                        ?: e("Unable to notify onAdClosed() because listener is null")

                    ad.showRequest = null
                    ad.invalidate()
                }

                override fun onExpired(partnerAd: PartnerAd) {
                    ad.listener?.onAdExpired(ad)
                        ?: e("Unable to notify onAdExpired() because listener is null")

                    ad.showRequest = null
                    ad.invalidate()
                }
            }

        private fun ChartboostMediationFullscreenAd.updateAdDetails(
            ad: CachedAd,
            loadId: String,
            listener: ChartboostMediationFullscreenAdListener,
            request: ChartboostMediationFullscreenAdLoadRequest,
        ) {
            this.cachedAd = ad
            this.loadId = loadId
            this.listener = listener
            this.request = request
            this.winningBidInfo = ad.winningBidInfo
            this.customData = ad.customData
        }
    }

    /**
     * The custom data for the ad.
     */
    var customData: String? = null
        set(value) {
            field =
                if (value != null && value.length > CUSTOM_DATA_MAX_CHAR) {
                    LogController.w("Failed to set custom data. It is longer than the maximum limit of $CUSTOM_DATA_MAX_CHAR characters.")
                    null
                } else {
                    value?.also { newValue ->
                        cachedAd?.customData = newValue
                    }
                }
        }

    private var showRequest: Job? = null

    /**
     * Show the fullscreen ad. This method is designed to be called from Java.
     *
     * @param activity The [Activity] with which to show the ad.
     * @param listener The [ChartboostMediationFullscreenAdShowListener] to notify of the ad show event.
     */
    fun showFullscreenAdFromJava(
        activity: Activity,
        listener: ChartboostMediationFullscreenAdShowListener,
    ) {
        CoroutineScope(Main).launch {
            listener.onAdShown(show(activity))
        }
    }

    /**
     * Show the fullscreen ad.
     *
     * @param activity The [Activity] with which to show the ad.
     *
     * @return The [ChartboostMediationAdShowResult] for the ad show event.
     */
    suspend fun show(activity: Activity): ChartboostMediationAdShowResult {
        if (isShowInProgress()) {
            return createFailureShowResult(ChartboostMediationError.ShowError.ShowInProgress)
        }

        return showAd(
            activity,
            cachedAd ?: run {
                invalidate()

                return createFailureShowResult(
                    ChartboostMediationError.ShowError.AdNotReady,
                )
            },
        )
    }

    /**
     * Invalidate the ad. This should be called when the ad is no longer needed.
     */
    fun invalidate() {
        // Don't invalidate if a show is in progress, as the ad still needs to fire the necessary callbacks, e.g. onAdClosed().
        if (isShowInProgress()) {
            d("Unable to invalidate ad because it is currently showing.")
            return
        }

        listener = null
        cachedAd = null
        showRequest?.cancel()
        showRequest = null
    }

    /**
     * Check if an ad show is in progress.
     */
    private fun isShowInProgress() = showRequest != null

    private suspend fun showAd(
        activity: Activity,
        showingAd: CachedAd,
    ): ChartboostMediationAdShowResult {
        var showResult = createFailureShowResult(ChartboostMediationError.ShowError.Unknown)

        showRequest =
            CoroutineScope(Main)
                .launch(
                    CoroutineExceptionHandler { _, error ->
                        showResult =
                            createFailureShowResult(
                                if (error is ChartboostMediationAdException) {
                                    error.chartboostMediationError
                                } else {
                                    ChartboostMediationError.ShowError.Exception
                                },
                            )
                        invalidate()
                    },
                ) {
                    showingAd.customData = customData ?: ""
                    showResult = adController?.show(activity, showingAd)?.apply {
                        if (error == null) {
                            CoroutineScope(Main)
                                .launch {
                                    listener?.onAdImpressionRecorded(this@ChartboostMediationFullscreenAd)
                                    ChartboostMediationSdk.chartboostMediationInternal.fullscreenAdShowingState.notifyFullscreenAdShown()
                                }.also { it.join() }
                        }
                    } ?: run {
                        invalidate()
                        createFailureShowResult(ChartboostMediationError.ShowError.NotInitialized)
                    }
                }.also { it.join() }

        return showResult
    }

    @OptIn(InternalSerializationApi::class)
    private fun createFailureShowResult(error: ChartboostMediationError?): ChartboostMediationAdShowResult {
        val metricsSet =
            setOf(
                Metrics(
                    event = Endpoints.Event.SHOW,
                    partner = null,
                ).apply {
                    start = System.currentTimeMillis()
                    end = System.currentTimeMillis()
                    duration = 0
                    isSuccess = false
                    chartboostMediationError = error
                    chartboostMediationErrorMessage = error?.message
                },
            )

        val metricsRequestBody = MetricsManager.buildMetricsDataRequestBody(metricsSet)
        val payloadJson =
            ChartboostMediationJson
                .writeJson(
                    metricsRequestBody,
                    MetricsRequestBody.serializer(),
                ).jsonObject
                .toJSONObject()

        return ChartboostMediationAdShowResult(payloadJson, error)
    }
}
