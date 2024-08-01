/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.view.View

data class PartnerAd(
    val ad: Any?,
    val details: Map<String, Any>,
    val request: PartnerAdLoadRequest,
    val partnerBannerSize: PartnerBannerSize? = null,
) {
    val inlineView: View? = ad as? View
}
