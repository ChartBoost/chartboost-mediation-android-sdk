/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context
import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.controllers.AdController
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.network.Endpoints
import com.chartboost.heliumsdk.network.model.MetricsRequestBody
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.HeliumJson
import com.chartboost.heliumsdk.utils.LogController
import com.chartboost.heliumsdk.utils.LogController.d
import com.chartboost.heliumsdk.utils.LogController.e
import com.chartboost.heliumsdk.utils.toJSONObject
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
 * @property request The publisher-supplied [ChartboostMediationAdLoadRequest] that was used to load the ad.
 * @property loadId The identifier for this load call.
 * @property winningBidInfo The winning bid info for the ad.
 * @property adController The ad controller to show the ad.
 */
class ChartboostMediationFullscreenAd(
    var cachedAd: CachedAd?,
    var listener: ChartboostMediationFullscreenAdListener?,
    var request: ChartboostMediationAdLoadRequest,
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

        internal suspend fun loadFullscreenAd(
            context: Context,
            request: ChartboostMediationAdLoadRequest,
            adController: AdController?,
            listener: ChartboostMediationFullscreenAdListener,
        ): ChartboostMediationFullscreenAdLoadResult = withContext(Main) {
            val loadId = generateLoadId()
            val ad = createChartboostMediationFullscreenAd(loadId, request, adController, listener)
            val adFormat = getAdFormat(request) ?: return@withContext createAdLoadResult(
                null,
                loadId,
                createPayloadJson(mutableSetOf()),
                ChartboostMediationError.CM_LOAD_FAILURE_UNSUPPORTED_AD_FORMAT
            )
            val adLoadParams = createAdLoadParams(ad, request, loadId, adFormat)
            val (metricsSet, loadResult) = performAdLoad(context, adLoadParams, adController)
            val payloadJson = createPayloadJson(metricsSet)
            val error = getError(loadResult)

            if (error != null) {
                e("Failed to load fullscreen ad with error: $error")
                return@withContext createAdLoadResult(null, loadId, payloadJson, error)
            }

            val cachedAd = getCachedAd(loadResult)

            ad.updateAdDetails(cachedAd, loadId, listener, request)
            createAdLoadResult(ad, loadId, payloadJson, null)
        }

        private fun generateLoadId(): String {
            return "${Environment.sessionId}${System.currentTimeMillis()}"
        }

        private fun createChartboostMediationFullscreenAd(
            loadId: String,
            request: ChartboostMediationAdLoadRequest,
            adController: AdController?,
            listener: ChartboostMediationFullscreenAdListener,
        ): ChartboostMediationFullscreenAd {
            return ChartboostMediationFullscreenAd(
                cachedAd = null,
                listener = listener,
                request = request,
                loadId = loadId,
                adController = adController
            )
        }

        private fun getAdFormat(request: ChartboostMediationAdLoadRequest): AdFormat? {
            return AppConfigStorage.placementsToAdFormats?.get(request.placementName)
        }

        private fun createAdLoadParams(
            ad: ChartboostMediationFullscreenAd,
            request: ChartboostMediationAdLoadRequest,
            loadId: String,
            adFormat: AdFormat,
        ): AdLoadParams {
            return AdLoadParams(
                adIdentifier = AdIdentifier(AdFormat.toAdType(adFormat), request.placementName),
                keywords = request.keywords,
                loadId = loadId,
                bannerSize = null,
                adInteractionListener = createAdInteractionListener(ad)
            )
        }

        private suspend fun performAdLoad(
            context: Context,
            adLoadParams: AdLoadParams,
            adController: AdController?,
        ): Pair<MutableSet<Metrics>, Result<CachedAd>?> = run {
            val metricsSet = mutableSetOf<Metrics>()
            var loadResult: Result<CachedAd>? = null

            CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
                loadResult = Result.failure(error)
            }) {
                loadResult = adController?.load(context, adLoadParams, metricsSet)
                    ?: Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED))
            }.also { it.join() }

            return Pair(metricsSet, loadResult)
        }

        @OptIn(InternalSerializationApi::class)
        private fun createPayloadJson(metricsSet: MutableSet<Metrics>): JSONObject {
            val metricsRequestBody = LogController.buildMetricsDataRequestBody(metricsSet)
            return HeliumJson.writeJson(
                metricsRequestBody,
                MetricsRequestBody.serializer()
            ).jsonObject.toJSONObject()
        }

        private fun getError(loadResult: Result<CachedAd>?): ChartboostMediationError? {
            return when (loadResult) {
                null -> ChartboostMediationError.CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED
                else -> loadResult.fold({ null }, { throwable ->
                    if (throwable is ChartboostMediationAdException) throwable.chartboostMediationError
                    else ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION
                })
            }
        }

        private fun getCachedAd(loadResult: Result<CachedAd>?): CachedAd {
            return loadResult?.getOrNull() ?: throw ChartboostMediationAdException(
                ChartboostMediationError.CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED
            )
        }

        private fun createAdLoadResult(
            ad: ChartboostMediationFullscreenAd?,
            loadId: String,
            payloadJson: JSONObject,
            error: ChartboostMediationError?,
        ) = ChartboostMediationFullscreenAdLoadResult(
            ad = ad,
            loadId = loadId,
            metrics = payloadJson,
            error = error
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
                    HeliumSdk.chartboostMediationInternal.fullscreenAdShowingState.notifyFullscreenAdClosed()
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
            request: ChartboostMediationAdLoadRequest,
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
            field = if (value != null && value.length > CUSTOM_DATA_MAX_CHAR) {
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
     * @param context The current [Context] with which to show the ad, recommended to be the current Activity.
     * @param listener The [ChartboostMediationFullscreenAdShowListener] to notify of the ad show event.
     */
    fun showFullscreenAdFromJava(
        context: Context,
        listener: ChartboostMediationFullscreenAdShowListener,
    ) {
        CoroutineScope(Main).launch {
            listener.onAdShown(show(context))
        }
    }

    /**
     * Show the fullscreen ad.
     *
     * @param context The current [Context] with which to show the ad, recommended to be the current Activity.
     *
     * @return The [ChartboostMediationAdShowResult] for the ad show event.
     */
    suspend fun show(context: Context): ChartboostMediationAdShowResult {
        if (isShowInProgress()) {
            return createFailureShowResult(ChartboostMediationError.CM_SHOW_FAILURE_SHOW_IN_PROGRESS)
        }

        return showAd(
            context,
            cachedAd ?: run {
                invalidate()

                return createFailureShowResult(
                    ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_READY
                )
            }
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
        context: Context,
        showingAd: CachedAd,
    ): ChartboostMediationAdShowResult {
        var showResult = createFailureShowResult(ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN)

        showRequest = CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            showResult = createFailureShowResult(
                if (error is ChartboostMediationAdException) error.chartboostMediationError
                else ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION
            )
            invalidate()
        }) {
            showingAd.customData = customData ?: ""
            showResult = adController?.show(context, showingAd)?.apply {
                if (error == null) {
                    CoroutineScope(Main).launch {
                        listener?.onAdImpressionRecorded(this@ChartboostMediationFullscreenAd)
                    }.also { it.join() }
                }
            } ?: run {
                invalidate()
                createFailureShowResult(ChartboostMediationError.CM_SHOW_FAILURE_NOT_INITIALIZED)
            }
        }.also { it.join() }

        return showResult
    }

    @OptIn(InternalSerializationApi::class)
    private fun createFailureShowResult(error: ChartboostMediationError?): ChartboostMediationAdShowResult {
        val metricsSet = setOf(
            Metrics(
                event = Endpoints.Sdk.Event.SHOW,
                partner = null
            ).apply {
                start = System.currentTimeMillis()
                end = System.currentTimeMillis()
                duration = 0
                isSuccess = false
                chartboostMediationError = error
                chartboostMediationErrorMessage = error?.message
            }
        )

        val metricsRequestBody = LogController.buildMetricsDataRequestBody(metricsSet)
        val payloadJson = HeliumJson.writeJson(
            metricsRequestBody,
            MetricsRequestBody.serializer()
        ).jsonObject.toJSONObject()

        return ChartboostMediationAdShowResult(payloadJson, error)
    }
}
