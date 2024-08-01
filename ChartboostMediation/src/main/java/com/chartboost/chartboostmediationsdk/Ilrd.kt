/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.os.Handler
import android.os.Looper
import org.json.JSONObject

/**
 * For impression level revenue data.
 */
class Ilrd {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val observers: MutableSet<ChartboostMediationIlrdObserver> = mutableSetOf()

    /**
     * @suppress
     *
     * Subscribe your observer for ILRD.
     */
    fun subscribe(observer: ChartboostMediationIlrdObserver) {
        handler.post {
            observers.add(observer)
        }
    }

    /**
     * @suppress
     *
     * Unsubscribe your observer for ILRD.
     */
    fun unsubscribe(observer: ChartboostMediationIlrdObserver) {
        handler.post {
            observers.remove(observer)
        }
    }

    /**
     * This is for internal use. Call this to trigger an ILRD callback for all registered observers.
     *
     * @param chartboostMediationPlacement The Chartboost Mediation placement triggering the ILRD callback
     * @param ilrdJson The actual ILRD data
     */
    internal fun onIlrdReceived(
        chartboostMediationPlacement: String,
        ilrdJson: JSONObject,
    ) {
        handler.post {
            observers.forEach { observer ->
                observer.onImpression(ChartboostMediationImpressionData(chartboostMediationPlacement, ilrdJson))
            }
        }
    }
}

interface ChartboostMediationIlrdObserver {
    fun onImpression(impData: ChartboostMediationImpressionData)
}

data class ChartboostMediationImpressionData(
    val placementId: String,
    val ilrdInfo: JSONObject,
)
