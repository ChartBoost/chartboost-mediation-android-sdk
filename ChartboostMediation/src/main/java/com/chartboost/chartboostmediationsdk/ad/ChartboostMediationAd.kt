/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import com.chartboost.chartboostmediationsdk.domain.Ad.AdType
import com.chartboost.chartboostmediationsdk.domain.Keywords

/**
 * @suppress
 *
 * The Chartboost Mediation SDK Ad interface.
 */
interface ChartboostMediationAd {
    /**
     * Storage to add key-value pairs for Chartboost Mediation's open RTB requests.
     */
    val keywords: Keywords

    /**
     * Publisher-specified map of String to Any that is sent to all the partners.
     */
    val partnerSettings: Map<String, Any>

    /**
     * Get the Chartboost Mediation Ad's placement
     */
    val placement: String

    /**
     * Get the ad type of this Chartboost Mediation Ad.
     * @return an integer representation of the ad type.
     */
    @AdType
    fun getAdType(): Int

    /**
     * Destroys the ad and does the necessary clean up and clears listeners.
     */
    fun destroy()
}
