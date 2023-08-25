/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import com.chartboost.heliumsdk.domain.Ad.AdType
import com.chartboost.heliumsdk.domain.Keywords

/**
 * @suppress
 *
 * The Helium SDK Ad interface.
 */
interface HeliumAd {

    /**
     * Storage to add key-value pairs for Helium's open RTB requests.
     */
    val keywords: Keywords

    /**
     * Get the Helium Ad's placement name
     */
     val placementName: String

    /**
     * Get the ad type of this Helium Ad.
     * @return an integer representation of the ad type.
     */
    @AdType
    fun getAdType(): Int

    /**
     * Load content for this ad.
     */
    fun load()

    /**
     * Destroys the ad and does the necessary clean up and clears listeners.
     */
    fun destroy()
}
