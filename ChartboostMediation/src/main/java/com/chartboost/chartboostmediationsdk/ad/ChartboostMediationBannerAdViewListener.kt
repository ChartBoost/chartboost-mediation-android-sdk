/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.view.View

/**
 * Use to listen to Chartboost Mediation banner ad events.
 */
@JvmDefaultWithCompatibility
interface ChartboostMediationBannerAdViewListener {
    /**
     * Called when the ad executes its clickthrough. This may happen multiple times for the same ad.
     *
     * @param placement Indicates which placement was clicked.
     */
    fun onAdClicked(placement: String)

    /**
     * Called when an ad impression occurs. This signal is when Chartboost Mediation fires an impression and
     * is independent of any partner impression.
     *
     * @param placement Indicates which placement recorded an impression.
     */
    fun onAdImpressionRecorded(placement: String)

    /**
     * Called when an ad View is added to this ChartboostMediationBannerAd.
     *
     * @param placement Indicates which placement had a View added.
     * @param child View being added
     */
    fun onAdViewAdded(
        placement: String,
        child: View?,
    )
}
