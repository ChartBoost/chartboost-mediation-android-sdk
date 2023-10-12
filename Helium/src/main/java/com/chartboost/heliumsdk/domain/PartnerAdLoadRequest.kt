/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.util.Size

data class PartnerAdLoadRequest(
    /**
     * The server-keyed identifier for a particular partner.
     */
    val partnerId: String,

    /**
     * The Chartboost placement associated with this ad.
     */
    val chartboostPlacement: String,

    /**
     * The partner placement or ad unit id or zone id or equivalent associated with this ad.
     */
    val partnerPlacement: String,

    /**
     * Size in density-independent pixels of the banner ad. Can be null if not a banner.
     */
    val size: Size?,

    /**
     * The ad format.
     */
    val format: AdFormat,

    /**
     * Ad markup for bidding ads.
     */
    val adm: String?,

    /**
     *  Unique request identifier for this load request.
     */
    val identifier: String,

    /**
     * Publisher-supplied and server-provided parameters settings to be passed to the active adapter.
     * This feature currently only forwards server data. Local/publisher data needs top-level support.
     */
    val partnerSettings: Map<String, String>,

    /**
     * Forwards events from the partner ad to this listener after an ad is loaded.
     */
    val adInteractionListener: AdInteractionListener,

    /**
     * Forwards events from the partner ad to this listener after an ad is loaded.
     */
    val isAdaptiveBanner: Boolean = false
)
