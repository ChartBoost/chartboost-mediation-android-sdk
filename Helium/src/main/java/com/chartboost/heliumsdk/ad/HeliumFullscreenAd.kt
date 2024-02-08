/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.app.Activity
import android.content.Context
import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.domain.ChartboostMediationError.CM_SHOW_FAILURE_NOT_INITIALIZED
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.coroutines.*
import org.json.JSONObject
import java.lang.ref.WeakReference

@Deprecated("Use ChartboostMediationFullscreenAd for the most comprehensive fullscreen ad experience.")
abstract class HeliumFullscreenAd(context: Context, override val placementName: String) : HeliumAd {
    protected var listener: HeliumFullscreenAdListener? = null
    protected var cachedAd: CachedAd? = null
    private var inflightRequest: Job? = null
    private val weakActivity: WeakReference<Activity?> = WeakReference(context as? Activity)
    private val appContext = context.applicationContext
    private val fullscreenAdShowingState
        get() = HeliumSdk.chartboostMediationInternal.fullscreenAdShowingState

    /**
     * Create a new [Keywords] on which keywords can be set.
     */
    override val keywords = Keywords()

    /**
     * Load content for this ad.
     */
    override fun load() {
        val context = weakActivity.get() ?: appContext
        if (inflightRequest != null) {
            LogController.d(ChartboostMediationError.CM_LOAD_FAILURE_LOAD_IN_PROGRESS.message)
            return
        }
        val previousLoadedAd = cachedAd
        if (previousLoadedAd != null) {
            CoroutineScope(Dispatchers.Main).launch {
                listener?.onAdCached(
                    placementName,
                    previousLoadedAd.loadId,
                    previousLoadedAd.winningBidInfo,
                    null,
                )
            }
            return
        }
        val loadId = Environment.sessionId + System.currentTimeMillis()
        inflightRequest =
            CoroutineScope(Dispatchers.Main).launch(
                CoroutineExceptionHandler { _, error ->
                    inflightRequest = null
                    CoroutineScope(Dispatchers.Main).launch {
                        listener?.onAdCached(
                            placementName,
                            loadId,
                            mapOf(),
                            if (error is ChartboostMediationAdException) {
                                error
                            } else {
                                ChartboostMediationAdException(
                                    ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION,
                                )
                            },
                        )
                    }
                },
            ) {
                val loadResult =
                    HeliumSdk.chartboostMediationInternal.adController?.load(
                        context,
                        AdLoadParams(
                            adIdentifier = AdIdentifier(getAdType(), placementName),
                            keywords = keywords,
                            loadId = loadId,
                            bannerSize = null,
                            adInteractionListener =
                                object : AdInteractionListener {
                                    override fun onImpressionTracked(partnerAd: PartnerAd) {
                                        // We ignore the partner impression
                                    }

                                    override fun onClicked(partnerAd: PartnerAd) {
                                        listener?.onAdClicked(partnerAd.request.chartboostPlacement)
                                    }

                                    override fun onRewarded(partnerAd: PartnerAd) {
                                        listener?.onAdRewarded(partnerAd.request.chartboostPlacement)
                                    }

                                    override fun onDismissed(
                                        partnerAd: PartnerAd,
                                        error: ChartboostMediationAdException?,
                                    ) {
                                        fullscreenAdShowingState?.notifyFullscreenAdClosed()
                                        listener?.onAdClosed(placementName, error)
                                    }

                                    override fun onExpired(partnerAd: PartnerAd) {
                                        cachedAd = null
                                    }
                                },
                        ),
                        // This is a placeholder set of load metrics that's effected by the new fullscreen load API talking to AdController.
                        // This is an internal change and has no visible effect for publishers.
                        mutableSetOf(),
                    )
                if (!isActive) {
                    inflightRequest = null
                    return@launch
                }
                loadResult?.fold({
                    it.loadId = loadId
                    cachedAd = it
                    CoroutineScope(Dispatchers.Main).launch {
                        inflightRequest = null
                        listener?.onAdCached(placementName, loadId, it.winningBidInfo, null)
                    }
                }, {
                    CoroutineScope(Dispatchers.Main).launch {
                        inflightRequest = null
                        listener?.onAdCached(
                            placementName,
                            loadId,
                            mapOf(),
                            if (it is ChartboostMediationAdException) {
                                it
                            } else {
                                ChartboostMediationAdException(
                                    ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION,
                                )
                            },
                        )
                    }
                }) ?: run {
                    LogController.e("Helium is not initialized.")
                    CoroutineScope(Dispatchers.Main).launch {
                        inflightRequest = null
                        listener?.onAdCached(
                            placementName,
                            loadId,
                            mapOf(),
                            ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_CHARTBOOST_MEDIATION_NOT_INITIALIZED),
                        )
                    }
                }
            }
    }

    /**
     * Show this ad
     */
    fun show() {
        val context = weakActivity.get() ?: appContext
        val showingAd =
            cachedAd ?: run {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onAdShown(
                        placementName,
                        ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_READY),
                    )
                }
                return
            }
        cachedAd = null
        CoroutineScope(Dispatchers.Main).launch(
            CoroutineExceptionHandler { _, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onAdShown(
                        placementName,
                        if (error is ChartboostMediationAdException) {
                            error
                        } else {
                            ChartboostMediationAdException(
                                ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION,
                            )
                        },
                    )
                }
            },
        ) {
            val showResult =
                HeliumSdk.chartboostMediationInternal.adController?.show(context, showingAd)
                    ?: ChartboostMediationAdShowResult(
                        JSONObject().apply {
                            Metrics(
                                partner = null,
                                event = Event.SHOW,
                            ).apply {
                                start = System.currentTimeMillis()
                                end = System.currentTimeMillis()
                                duration = 0
                                isSuccess = false
                                chartboostMediationError = CM_SHOW_FAILURE_NOT_INITIALIZED
                                chartboostMediationErrorMessage =
                                    CM_SHOW_FAILURE_NOT_INITIALIZED.message
                            }
                        },
                        CM_SHOW_FAILURE_NOT_INITIALIZED,
                    )

            showResult.error?.let { error ->
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onAdShown(
                        placementName,
                        ChartboostMediationAdException(error),
                    )
                }
            } ?: run {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onAdShown(placementName, null)
                    listener?.onAdImpressionRecorded(placementName)
                    fullscreenAdShowingState?.notifyFullscreenAdShown()
                }
            }
        }
    }

    /**
     * Check if an ad is ready to be shown.
     * @return if the ad is ready to show at a given moment
     */
    fun readyToShow(): Boolean {
        return cachedAd != null
    }

    /**
     * Clear an ad that has successfully loaded so it can be loaded again. Also cancels any in-flight
     * load requests.
     *
     * @return if the ad has been successfully cleared
     */
    fun clearLoaded() {
        inflightRequest?.cancel()
        inflightRequest = null
        cachedAd = null
    }

    /**
     * Destroys the ad and does the necessary clean up and clears listeners.
     */
    override fun destroy() {
        clearLoaded()
        listener = null
        weakActivity.clear()
    }
}
