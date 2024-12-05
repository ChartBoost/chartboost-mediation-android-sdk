/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.os.Handler
import android.os.Looper
import org.json.JSONObject

/**
 * For impression level revenue data.
 */
class Ilrd {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val observers: MutableSet<HeliumIlrdObserver> = mutableSetOf()

    /**
     * @suppress
     *
     * Subscribe your observer for ILRD.
     */
    fun subscribe(observer: HeliumIlrdObserver) {
        handler.post {
            observers.add(observer)
        }
    }

    /**
     * @suppress
     *
     * Unsubscribe your observer for ILRD.
     */
    fun unsubscribe(observer: HeliumIlrdObserver) {
        handler.post {
            observers.remove(observer)
        }
    }

    /**
     * This is for internal use. Call this to trigger an ILRD callback for all registered observers.
     *
     * @param heliumPlacement The Helium placement triggering the ILRD callback
     * @param ilrdJson The actual ILRD data
     */
    internal fun onIlrdReceived(
        heliumPlacement: String,
        ilrdJson: JSONObject,
    ) {
        handler.post {
            observers.forEach { observer ->
                observer.onImpression(HeliumImpressionData(heliumPlacement, ilrdJson))
            }
        }
    }
}

interface HeliumIlrdObserver {
    fun onImpression(impData: HeliumImpressionData)
}

data class HeliumImpressionData(val placementId: String, val ilrdInfo: JSONObject)
